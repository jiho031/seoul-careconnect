package com.seoulcareconnect.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulcareconnect.config.ai.AiProperties;
import com.seoulcareconnect.dto.ai.AiPolicyExplanationDto;
import com.seoulcareconnect.entity.ai.AiPolicyExplanation;
import com.seoulcareconnect.entity.ai.AiPolicyExplanation.ReviewStatus;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.PolicyDetail;
import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.repository.ai.AiPolicyExplanationRepository;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiPolicyExplanationService {

    private static final int FIELD_LIMIT = 8_000;
    private static final String SYSTEM_PROMPT = """
            당신은 서울시 정책 정보를 시민이 이해하기 쉬운 한국어로 바꾸는 행정 정보 편집자입니다.
            제공된 정책 원문만 근거로 사용하세요. 원문에 없는 자격, 금액, 날짜, 신청 링크를 추측하거나 만들지 마세요.
            나이·소득·거주지·기간·금액 등 숫자 조건은 원문과 정확히 일치시켜야 합니다.
            불명확하거나 없는 정보는 '공식 공고에서 확인이 필요합니다'라고 명시하세요.
            신청 가능 여부를 확정적으로 판정하지 말고, 쉬운 존댓말을 사용하세요.
            각 항목은 중복 없이 1~3개의 짧은 문장으로 작성하세요.
            """;

    private final AiPolicyExplanationRepository explanationRepository;
    private final PolicyRepository policyRepository;
    private final AiModelGateway modelGateway;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;
    private final AiSummarySettingService settingService;

    public Optional<AiPolicyExplanationDto> findGenerated(Long policyId) {
        return explanationRepository
                .findFirstByPolicyPolicyIdAndReviewStatusOrderByCreatedAtDesc(
                        policyId,
                        ReviewStatus.APPROVED
                )
                .map(this::toDto);
    }

    public Page<AiPolicyExplanationDto> findGeneratedPage(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 50));

        return explanationRepository
                .findActiveGeneratedPage(
                        ReviewStatus.APPROVED,
                        ApplyStatus.EXPIRED,
                        today(),
                        PageRequest.of(safePage, safeSize)
                )
                .map(this::toDto);
    }

    public List<AiPolicyExplanationDto> findRecentGenerated() {
        return explanationRepository
                .findTop100ByReviewStatusOrderByCreatedAtDesc(ReviewStatus.APPROVED)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public long countGenerated() {
        return explanationRepository.countActiveGenerated(
                ReviewStatus.APPROVED,
                ApplyStatus.EXPIRED,
                today()
        );
    }

    public long countGeneratedSince(LocalDateTime since) {
        return explanationRepository.countActiveGeneratedSince(
                ReviewStatus.APPROVED,
                ApplyStatus.EXPIRED,
                today(),
                since
        );
    }

    @Transactional
    public synchronized AiPolicyExplanationDto getOrCreate(Long policyId) {
        Optional<AiPolicyExplanation> existing = explanationRepository
                .findFirstByPolicyPolicyIdAndReviewStatusOrderByCreatedAtDesc(
                        policyId,
                        ReviewStatus.APPROVED
                );
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        if (!settingService.isAutomaticSummaryEnabled()) {
            throw new GenerationDisabledException(
                    "관리자가 자동 AI 요약을 꺼 둔 상태입니다. 요약이 준비된 뒤 다시 이용해 주세요."
            );
        }

        return createForPolicy(policyId);
    }

    @Transactional
    public synchronized AiPolicyExplanationDto generateIfMissing(Long policyId) {
        Optional<AiPolicyExplanation> existing = explanationRepository
                .findFirstByPolicyPolicyIdAndReviewStatusOrderByCreatedAtDesc(
                        policyId,
                        ReviewStatus.APPROVED
                );
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        return createForPolicy(policyId);
    }

    private AiPolicyExplanationDto createForPolicy(Long policyId) {
        Policy policy = policyRepository.findWithSourceAndDetailByPolicyId(policyId)
                .orElseThrow(() -> new IllegalArgumentException("정책을 찾을 수 없습니다. ID=" + policyId));
        return createGenerated(policy);
    }

    @Transactional
    public AiPolicyExplanationDto update(
            Long explanationId,
            String easySummary,
            String eligibilitySummary,
            String benefitSummary,
            String applicationSummary,
            String cautionSummary,
            String editor
    ) {
        AiPolicyExplanation target = findExplanation(explanationId);
        if (target.getReviewStatus() != ReviewStatus.APPROVED) {
            throw new IllegalStateException("현재 사용 중인 AI 요약만 수정할 수 있습니다.");
        }

        AiPolicyExplanation.Content content = new AiPolicyExplanation.Content(
                normalizedField(easySummary),
                normalizedField(eligibilitySummary),
                normalizedField(benefitSummary),
                normalizedField(applicationSummary),
                normalizedField(cautionSummary)
        );
        validateContent(content);
        target.updateContent(content, editor);
        return toDto(target);
    }

    @Transactional
    public void delete(Long explanationId) {
        AiPolicyExplanation target = findExplanation(explanationId);
        explanationRepository.delete(target);
    }

    public boolean isOpenAiConfigured() {
        return modelGateway.isOpenAiConfigured();
    }

    public String modelName() {
        return modelGateway.openAiModelName();
    }

    private AiPolicyExplanationDto createGenerated(Policy policy) {
        GeneratedContent generated = generateContent(policy);
        AiPolicyExplanation saved = explanationRepository.save(
                AiPolicyExplanation.generated(
                        policy,
                        generated.content(),
                        generated.modelName(),
                        properties.getPromptVersion()
                )
        );
        return toDto(saved);
    }

    private GeneratedContent generateContent(Policy policy) {
        AiModelGateway.GeneratedJson generated = modelGateway.generateJsonOpenAiOnly(
                "policy_easy_explanation",
                SYSTEM_PROMPT,
                List.of(new AiModelGateway.Message("user", buildPolicySource(policy))),
                explanationSchema(),
                properties.getMaxOutputTokens(),
                List.of(
                        "easySummary",
                        "eligibilitySummary",
                        "benefitSummary",
                        "applicationSummary",
                        "cautionSummary"
                )
        );

        try {
            AiPolicyExplanation.Content content = objectMapper.treeToValue(
                    generated.payload(),
                    AiPolicyExplanation.Content.class
            );
            validateContent(content);
            return new GeneratedContent(content, generated.modelName());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 응답을 정책 설명 형식으로 변환하지 못했습니다.", exception);
        }
    }

    private Map<String, Object> explanationSchema() {
        Map<String, Object> fields = Map.of(
                "easySummary", textField("정책의 목적과 핵심 지원을 쉬운 말로 요약"),
                "eligibilitySummary", textField("지원 대상과 핵심 자격 조건을 쉬운 말로 요약"),
                "benefitSummary", textField("지원 내용과 금액·횟수 조건을 쉬운 말로 요약"),
                "applicationSummary", textField("신청 방법·기간·문의처를 쉬운 말로 요약"),
                "cautionSummary", textField("누락 정보와 공식 공고 확인이 필요한 주의사항")
        );

        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", fields,
                "required", List.of(
                        "easySummary",
                        "eligibilitySummary",
                        "benefitSummary",
                        "applicationSummary",
                        "cautionSummary"
                )
        );
    }

    private Map<String, Object> textField(String description) {
        return Map.of("type", "string", "description", description);
    }

    private String buildPolicySource(Policy policy) {
        PolicyDetail detail = policy.getDetail();
        String sourceName = policy.getSource() == null ? null : policy.getSource().getSourceName();

        return """
                다음 정책 원문을 시민용 쉬운 설명으로 정리하세요.

                [정책명] %s
                [제공기관] %s
                [분야] %s
                [지원 대상] %s
                [지역] %s %s
                [신청 시작일] %s
                [신청 종료일] %s
                [신청 상태] %s
                [신청 방법] %s
                [문의처] %s
                [공식 URL] %s
                [지원 내용] %s
                [선정 기준] %s
                [필요 서류] %s
                [상세 원문] %s
                """.formatted(
                safe(policy.getTitle()),
                safe(sourceName),
                safe(policy.getCategory()),
                safe(policy.getTarget()),
                safe(policy.getRegion()),
                safe(policy.getDistrict()),
                safe(policy.getStartDate()),
                safe(policy.getEndDate()),
                safe(policy.getApplyStatus()),
                safe(policy.getApplyMethod()),
                safe(policy.getContact()),
                safe(policy.getOfficialUrl()),
                safe(detail == null ? null : detail.getBenefit()),
                safe(detail == null ? null : detail.getSelectionCriteria()),
                safe(detail == null ? null : detail.getRequiredDocumentsText()),
                safe(detail == null ? null : detail.getContentText())
        );
    }

    private String safe(Object value) {
        if (value == null) return "정보 없음";
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return "정보 없음";
        return text.length() <= FIELD_LIMIT ? text : text.substring(0, FIELD_LIMIT);
    }

    private void validateContent(AiPolicyExplanation.Content content) {
        if (content == null
                || !StringUtils.hasText(content.easySummary())
                || !StringUtils.hasText(content.eligibilitySummary())
                || !StringUtils.hasText(content.benefitSummary())
                || !StringUtils.hasText(content.applicationSummary())
                || !StringUtils.hasText(content.cautionSummary())) {
            throw new IllegalStateException("AI가 필수 정책 설명 항목을 모두 생성하지 못했습니다.");
        }
    }

    private String normalizedField(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("AI 요약의 모든 항목을 입력해 주세요.");
        }
        String normalized = value.trim();
        if (normalized.length() > FIELD_LIMIT) {
            throw new IllegalArgumentException("AI 요약 항목은 8,000자 이하로 입력해 주세요.");
        }
        return normalized;
    }

    private AiPolicyExplanation findExplanation(Long explanationId) {
        return explanationRepository.findWithPolicyByExplanationId(explanationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "AI 정책 설명을 찾을 수 없습니다. ID=" + explanationId
                ));
    }


    private LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Seoul"));
    }

    private AiPolicyExplanationDto toDto(AiPolicyExplanation explanation) {
        return new AiPolicyExplanationDto(
                explanation.getExplanationId(),
                explanation.getPolicy().getPolicyId(),
                explanation.getPolicy().getTitle(),
                explanation.getReviewStatus(),
                explanation.getReviewStatus().getLabel(),
                explanation.getEasySummary(),
                explanation.getEligibilitySummary(),
                explanation.getBenefitSummary(),
                explanation.getApplicationSummary(),
                explanation.getCautionSummary(),
                explanation.getModelName(),
                explanation.getPromptVersion(),
                explanation.getReviewComment(),
                explanation.getReviewedBy(),
                explanation.getSourceUpdatedAt(),
                explanation.getCreatedAt(),
                explanation.getReviewedAt()
        );
    }

    private record GeneratedContent(AiPolicyExplanation.Content content, String modelName) {
    }

    public static class GenerationDisabledException extends IllegalStateException {
        public GenerationDisabledException(String message) {
            super(message);
        }
    }
}
