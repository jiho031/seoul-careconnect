package com.seoulcareconnect.service.ai;

import com.seoulcareconnect.dto.ai.AiPolicySummaryStatusResponse;
import com.seoulcareconnect.service.policy.PolicyDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AiPolicySummaryGenerationService {

    private static final int TOTAL_STAGES = 4;

    private final AiPolicyExplanationService explanationService;
    private final PolicyDetailService policyDetailService;
    private final TaskExecutor taskExecutor;
    private final Map<Long, SummaryJob> jobs = new ConcurrentHashMap<>();

    public AiPolicySummaryGenerationService(
            AiPolicyExplanationService explanationService,
            PolicyDetailService policyDetailService,
            @Qualifier("aiPolicySummaryRequestExecutor") TaskExecutor taskExecutor
    ) {
        this.explanationService = explanationService;
        this.policyDetailService = policyDetailService;
        this.taskExecutor = taskExecutor;
    }

    public synchronized AiPolicySummaryStatusResponse start(Long policyId) {
        policyDetailService.get(policyId);

        if (explanationService.findGenerated(policyId).isPresent()) {
            SummaryJob completed = SummaryJob.completed();
            jobs.put(policyId, completed);
            return toResponse(policyId, completed);
        }

        SummaryJob current = jobs.get(policyId);
        if (current != null && current.running()) {
            return toResponse(policyId, current);
        }

        if (!explanationService.isOpenAiConfigured()) {
            throw new SummaryUnavailableException(
                    "AI 요약을 생성하려면 서버에 OPENAI_API_KEY를 설정해 주세요."
            );
        }

        SummaryJob queued = SummaryJob.queued();
        jobs.put(policyId, queued);

        try {
            taskExecutor.execute(() -> generate(policyId));
        } catch (RuntimeException exception) {
            jobs.put(policyId, SummaryJob.failed(
                    queued.stage(),
                    "요약 생성 작업을 시작하지 못했습니다. 잠시 후 다시 시도해 주세요."
            ));
            throw new SummaryUnavailableException(
                    "요약 생성 작업을 시작하지 못했습니다. 잠시 후 다시 시도해 주세요.",
                    exception
            );
        }

        return toResponse(policyId, jobs.get(policyId));
    }

    public AiPolicySummaryStatusResponse status(Long policyId) {
        SummaryJob current = jobs.get(policyId);
        if (current != null && current.state() != State.COMPLETED) {
            return toResponse(policyId, current);
        }

        if (explanationService.findGenerated(policyId).isPresent()) {
            SummaryJob completed = SummaryJob.completed();
            jobs.put(policyId, completed);
            return toResponse(policyId, completed);
        }

        jobs.remove(policyId);
        return toResponse(policyId, SummaryJob.idle());
    }

    private void generate(Long policyId) {
        update(policyId, SummaryJob.generating());

        try {
            explanationService.generateIfMissing(
                    policyId,
                    () -> update(policyId, SummaryJob.saving())
            );
            update(policyId, SummaryJob.completed());
        } catch (RuntimeException exception) {
            SummaryJob current = jobs.getOrDefault(policyId, SummaryJob.generating());
            String message = userMessage(exception);
            update(policyId, SummaryJob.failed(current.stage(), message));
            log.warn("사용자 요청 AI 요약 생성 실패: policyId={}", policyId, exception);
        }
    }

    private void update(Long policyId, SummaryJob job) {
        jobs.put(policyId, job);
    }

    private AiPolicySummaryStatusResponse toResponse(Long policyId, SummaryJob job) {
        return new AiPolicySummaryStatusResponse(
                policyId,
                job.state().name(),
                job.state().label,
                job.stage(),
                TOTAL_STAGES,
                job.message()
        );
    }

    private String userMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "AI 요약을 생성하지 못했습니다. 잠시 후 다시 시도해 주세요.";
        }
        return message.length() <= 300 ? message : message.substring(0, 300);
    }

    private enum State {
        IDLE("생성 준비"),
        QUEUED("요청 접수"),
        GENERATING("AI 요약 생성 중"),
        SAVING("요약 저장 중"),
        COMPLETED("생성 완료"),
        FAILED("생성 실패");

        private final String label;

        State(String label) {
            this.label = label;
        }
    }

    private record SummaryJob(
            State state,
            int stage,
            String message
    ) {
        private static SummaryJob idle() {
            return new SummaryJob(
                    State.IDLE,
                    0,
                    "버튼을 누르면 AI 요약 생성을 시작합니다."
            );
        }

        private static SummaryJob queued() {
            return new SummaryJob(
                    State.QUEUED,
                    1,
                    "AI 요약 생성 요청을 접수했습니다."
            );
        }

        private static SummaryJob generating() {
            return new SummaryJob(
                    State.GENERATING,
                    2,
                    "공식 정책 내용을 바탕으로 쉬운 요약을 만들고 있습니다."
            );
        }

        private static SummaryJob saving() {
            return new SummaryJob(
                    State.SAVING,
                    3,
                    "생성한 요약을 저장하고 있습니다."
            );
        }

        private static SummaryJob completed() {
            return new SummaryJob(
                    State.COMPLETED,
                    4,
                    "AI 요약 생성이 완료되었습니다. 요약 내용을 불러옵니다."
            );
        }

        private static SummaryJob failed(int stage, String message) {
            return new SummaryJob(State.FAILED, Math.max(0, stage), message);
        }

        private boolean running() {
            return state == State.QUEUED
                    || state == State.GENERATING
                    || state == State.SAVING;
        }
    }

    public static class SummaryUnavailableException extends IllegalStateException {
        public SummaryUnavailableException(String message) {
            super(message);
        }

        public SummaryUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
