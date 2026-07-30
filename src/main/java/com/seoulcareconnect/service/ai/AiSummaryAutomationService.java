package com.seoulcareconnect.service.ai;

import com.seoulcareconnect.entity.ai.AiPolicyExplanation.ReviewStatus;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyStatus;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class AiSummaryAutomationService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final List<PolicyStatus> ELIGIBLE_STATUSES = List.of(
            PolicyStatus.AUTO_PUBLISHED,
            PolicyStatus.PENDING_REVIEW,
            PolicyStatus.APPROVED,
            PolicyStatus.NEEDS_UPDATE
    );

    private final AiSummarySettingService settingService;
    private final AiPolicyExplanationService explanationService;
    private final PolicyRepository policyRepository;
    private final TaskExecutor taskExecutor;
    private final AtomicBoolean manualGenerationRunning = new AtomicBoolean(false);
    private final AtomicInteger processedCount = new AtomicInteger();
    private final AtomicInteger succeededCount = new AtomicInteger();
    private final AtomicInteger failedCount = new AtomicInteger();
    private volatile int requestedCount;
    private volatile String lastError;
    private volatile LocalDateTime startedAt;
    private volatile LocalDateTime completedAt;

    public AiSummaryAutomationService(
            AiSummarySettingService settingService,
            AiPolicyExplanationService explanationService,
            PolicyRepository policyRepository,
            @Qualifier("aiSummaryTaskExecutor") TaskExecutor taskExecutor
    ) {
        this.settingService = settingService;
        this.explanationService = explanationService;
        this.policyRepository = policyRepository;
        this.taskExecutor = taskExecutor;
    }

    public long countMissingSummaries() {
        return policyRepository.countWithoutAiExplanation(
                ELIGIBLE_STATUSES,
                ApplyStatus.EXPIRED,
                ReviewStatus.APPROVED,
                today()
        );
    }

    public StartResult startManualGeneration() {
        if (settingService.isAutomaticSummaryEnabled()) {
            throw new IllegalStateException("자동 AI 요약을 끈 뒤 수동 전체 요약을 실행해 주세요.");
        }
        if (!explanationService.isOpenAiConfigured()) {
            throw new IllegalStateException("OPENAI_API_KEY가 설정되어 있지 않아 AI 요약을 생성할 수 없습니다.");
        }
        if (!manualGenerationRunning.compareAndSet(false, true)) {
            throw new IllegalStateException("미요약 정책 전체 요약이 이미 진행 중입니다.");
        }

        try {
            List<Long> policyIds = policyRepository.findIdsWithoutAiExplanation(
                    ELIGIBLE_STATUSES,
                    ApplyStatus.EXPIRED,
                    ReviewStatus.APPROVED,
                    today()
            );
            resetStatus(policyIds.size());

            if (policyIds.isEmpty()) {
                completedAt = LocalDateTime.now();
                manualGenerationRunning.set(false);
                return new StartResult(0, false);
            }

            taskExecutor.execute(() -> generateMissing(policyIds));
            return new StartResult(policyIds.size(), true);
        } catch (RuntimeException exception) {
            manualGenerationRunning.set(false);
            throw exception;
        }
    }

    public BatchStatus manualGenerationStatus() {
        return new BatchStatus(
                manualGenerationRunning.get(),
                requestedCount,
                processedCount.get(),
                succeededCount.get(),
                failedCount.get(),
                lastError,
                startedAt,
                completedAt
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePolicyCreated(PolicyCreated event) {
        try {
            taskExecutor.execute(() -> generateAutomatically(event.policyId(), false));
        } catch (RuntimeException exception) {
            log.warn("자동 AI 요약 작업 등록 실패: policyId={}", event.policyId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePolicyUpdated(PolicyUpdated event) {
        try {
            taskExecutor.execute(() -> generateAutomatically(event.policyId(), true));
        } catch (RuntimeException exception) {
            log.warn("AI 요약 재생성 작업 등록 실패: policyId={}", event.policyId(), exception);
        }
    }

    private void generateAutomatically(Long policyId, boolean regenerate) {
        if (!settingService.isAutomaticSummaryEnabled()) return;

        Policy policy = policyRepository.findById(policyId).orElse(null);
        if (policy == null
                || !ELIGIBLE_STATUSES.contains(policy.getStatus())
                || policy.getApplyStatus() == ApplyStatus.EXPIRED) {
            return;
        }

        try {
            if (regenerate) {
                explanationService.regenerate(policyId);
            } else {
                explanationService.generateIfMissing(policyId);
            }
        } catch (RuntimeException exception) {
            log.warn(
                    regenerate ? "AI 요약 재생성 실패: policyId={}" : "자동 AI 요약 생성 실패: policyId={}",
                    policyId,
                    exception
            );
        }
    }

    private void generateMissing(List<Long> policyIds) {
        try {
            for (Long policyId : policyIds) {
                Policy policy = policyRepository.findById(policyId).orElse(null);
                if (!isManualCandidate(policy, today())) {
                    requestedCount = Math.max(0, requestedCount - 1);
                    continue;
                }

                try {
                    explanationService.generateIfMissing(policyId);
                    succeededCount.incrementAndGet();
                } catch (RuntimeException exception) {
                    failedCount.incrementAndGet();
                    lastError = limitedMessage(exception);
                    log.warn("수동 전체 AI 요약 생성 실패: policyId={}", policyId, exception);
                } finally {
                    processedCount.incrementAndGet();
                }
            }
        } finally {
            completedAt = LocalDateTime.now();
            manualGenerationRunning.set(false);
        }
    }

    private boolean isManualCandidate(Policy policy, LocalDate today) {
        return policy != null
                && ELIGIBLE_STATUSES.contains(policy.getStatus())
                && policy.getApplyStatus() != ApplyStatus.EXPIRED
                && (policy.getEndDate() == null || !policy.getEndDate().isBefore(today));
    }

    private LocalDate today() {
        return LocalDate.now(SEOUL_ZONE);
    }

    private void resetStatus(int count) {
        requestedCount = count;
        processedCount.set(0);
        succeededCount.set(0);
        failedCount.set(0);
        lastError = null;
        startedAt = LocalDateTime.now();
        completedAt = null;
    }

    private String limitedMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return "알 수 없는 오류";
        return message.length() <= 300 ? message : message.substring(0, 300);
    }

    public record PolicyCreated(Long policyId) {
    }

    public record PolicyUpdated(Long policyId) {
    }

    public record StartResult(int requestedCount, boolean started) {
    }

    public record BatchStatus(
            boolean running,
            int requestedCount,
            int processedCount,
            int succeededCount,
            int failedCount,
            String lastError,
            LocalDateTime startedAt,
            LocalDateTime completedAt
    ) {
    }
}
