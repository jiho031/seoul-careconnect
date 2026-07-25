package com.seoulcareconnect.service.impl.ai;

import com.seoulcareconnect.config.ai.AiProperties;
import com.seoulcareconnect.dto.ai.AiAssistantGroundingDocument;
import com.seoulcareconnect.dto.ai.AiAssistantMessage;
import com.seoulcareconnect.dto.ai.AiAssistantRequest;
import com.seoulcareconnect.dto.ai.AiAssistantResponse;
import com.seoulcareconnect.dto.ai.AiAssistantSourceDto;
import com.seoulcareconnect.dto.ai.AiAssistantUserContext;
import com.seoulcareconnect.entity.ai.AiPolicyExplanation;
import com.seoulcareconnect.entity.ai.AiReviewStatus;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.PolicyDetail;
import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyStatus;
import com.seoulcareconnect.integration.client.ai.OpenAiAssistantClient;
import com.seoulcareconnect.repository.ai.AiPolicyExplanationRepository;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import com.seoulcareconnect.service.ai.AiAssistantUnavailableException;
import com.seoulcareconnect.service.ai.AiPolicyAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiPolicyAssistantServiceImpl implements AiPolicyAssistantService {

    private static final List<PolicyStatus> PUBLIC_STATUSES = List.of(
            PolicyStatus.AUTO_PUBLISHED,
            PolicyStatus.APPROVED
    );
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final String NOTICE =
            "AI 답변은 신청 준비를 돕는 참고 정보입니다. 실제 자격과 신청 조건은 공식 정책 페이지에서 확인해 주세요.";

    private final PolicyRepository policyRepository;
    private final AiPolicyExplanationRepository explanationRepository;
    private final PolicyQuestionFilter questionFilter;
    private final AiPolicyEmbeddingService embeddingService;
    private final OpenAiAssistantClient assistantClient;
    private final AiProperties properties;

    @Value("${app.policy.sync.zone-id:Asia/Seoul}")
    private String zoneId;

    @Override
    public AiAssistantResponse ask(
            AiAssistantRequest request,
            AiAssistantUserContext userContext
    ) {
        String question = request.question().trim();
        String enrichedQuestion = questionFilter.enrichWithProfile(question, userContext);
        String retrievalQuestion = retrievalQuestion(enrichedQuestion, request.safeHistory());

        try {
            List<AiAssistantCandidate> candidates = request.policyId() == null
                    ? generalCandidates(retrievalQuestion)
                    : policyCandidate(request.policyId());

            if (candidates.isEmpty()) {
                return new AiAssistantResponse(
                        "현재 공개 중인 정책 정보에서 질문에 답할 근거를 찾지 못했습니다. 지역, 연령대, 관심 분야를 조금 더 구체적으로 적어 주세요.",
                        List.of(),
                        false,
                        NOTICE
                );
            }

            List<AiAssistantCandidate> ranked = request.policyId() == null
                    ? embeddingService.rank(retrievalQuestion, candidates)
                    : candidates;
            List<AiAssistantGroundingDocument> documents = withCitations(ranked);
            String answer = assistantClient.answer(
                    enrichedQuestion,
                    request.safeHistory(),
                    documents
            );

            return new AiAssistantResponse(
                    answer,
                    documents.stream().map(this::toSource).toList(),
                    true,
                    NOTICE
            );
        } catch (AiAssistantUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AiAssistantUnavailableException(
                    "AI 정책 도우미를 일시적으로 이용할 수 없습니다. 잠시 후 다시 시도해 주세요.",
                    exception
            );
        }
    }

    private List<AiAssistantCandidate> generalCandidates(String question) {
        List<Policy> policies = policyRepository.findAssistantCandidates(
                PUBLIC_STATUSES,
                ApplyStatus.EXPIRED,
                today()
        );

        List<Policy> filtered = policies.stream()
                .filter(policy -> questionFilter.matches(question, policy))
                .sorted(Comparator
                        .comparingInt((Policy policy) ->
                                questionFilter.lexicalScore(question, policy))
                        .reversed()
                        .thenComparing(
                                Policy::getViewCount,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ))
                .limit(Math.max(1, properties.getAssistantCandidateLimit()))
                .toList();

        Map<Long, AiPolicyExplanation> explanations = approvedExplanations(filtered);
        return filtered.stream()
                .map(policy -> new AiAssistantCandidate(
                        policy,
                        buildDocument(policy, explanations.get(policy.getPolicyId()), 0),
                        questionFilter.lexicalScore(question, policy)
                ))
                .toList();
    }

    private List<AiAssistantCandidate> policyCandidate(Long policyId) {
        return policyRepository.findAssistantPolicy(
                        policyId,
                        PUBLIC_STATUSES,
                        ApplyStatus.EXPIRED,
                        today()
                )
                .map(policy -> {
                    AiPolicyExplanation explanation = explanationRepository
                            .findFirstByPolicyPolicyIdAndReviewStatusOrderByCreatedAtDesc(
                                    policyId,
                                    AiReviewStatus.APPROVED
                            )
                            .orElse(null);
                    return List.of(new AiAssistantCandidate(
                            policy,
                            buildDocument(policy, explanation, 0),
                            0
                    ));
                })
                .orElseGet(List::of);
    }

    private Map<Long, AiPolicyExplanation> approvedExplanations(List<Policy> policies) {
        if (policies.isEmpty()) return Map.of();

        List<Long> policyIds = policies.stream().map(Policy::getPolicyId).toList();
        return explanationRepository.findByPolicyPolicyIdInAndReviewStatus(
                        policyIds,
                        AiReviewStatus.APPROVED
                )
                .stream()
                .collect(Collectors.toMap(
                        explanation -> explanation.getPolicy().getPolicyId(),
                        Function.identity(),
                        this::newerExplanation,
                        LinkedHashMap::new
                ));
    }

    private AiPolicyExplanation newerExplanation(
            AiPolicyExplanation left,
            AiPolicyExplanation right
    ) {
        if (left.getCreatedAt() == null) return right;
        if (right.getCreatedAt() == null) return left;
        return left.getCreatedAt().isAfter(right.getCreatedAt()) ? left : right;
    }

    private List<AiAssistantGroundingDocument> withCitations(
            List<AiAssistantCandidate> candidates
    ) {
        return java.util.stream.IntStream.range(0, candidates.size())
                .mapToObj(index -> withCitation(candidates.get(index).document(), index + 1))
                .toList();
    }

    private AiAssistantGroundingDocument withCitation(
            AiAssistantGroundingDocument document,
            int citation
    ) {
        return new AiAssistantGroundingDocument(
                citation,
                document.policyId(),
                document.title(),
                document.agency(),
                document.category(),
                document.target(),
                document.region(),
                document.applicationPeriod(),
                document.sourceUpdatedDate(),
                document.applicationMethod(),
                document.contact(),
                document.benefit(),
                document.selectionCriteria(),
                document.requiredDocuments(),
                document.content(),
                document.approvedEasySummary(),
                document.approvedEligibilitySummary(),
                document.approvedBenefitSummary(),
                document.approvedApplicationSummary(),
                document.approvedCautionSummary(),
                document.officialUrl(),
                document.embeddingText()
        );
    }

    private AiAssistantGroundingDocument buildDocument(
            Policy policy,
            AiPolicyExplanation explanation,
            int citation
    ) {
        PolicyDetail detail = policy.getDetail();
        String agency = policy.getSource() == null
                ? "공식 제공기관"
                : valueOr(policy.getSource().getSourceName(), "공식 제공기관");
        String category = policy.getCategory() == null
                ? "분야 확인 필요"
                : policy.getCategory().getLabel();
        String region = valueOr(
                policy.getDistrict(),
                valueOr(policy.getRegion(), "서울시 전체")
        );
        String officialUrl = safeOfficialUrl(policy.getOfficialUrl());
        String embeddingText = embeddingText(policy, detail, explanation, agency, category, region);

        return new AiAssistantGroundingDocument(
                citation,
                policy.getPolicyId(),
                valueOr(policy.getTitle(), "제목 없음"),
                agency,
                category,
                valueOr(policy.getTarget(), "공식 공고 확인"),
                region,
                applicationPeriod(policy),
                sourceUpdatedDate(policy),
                valueOr(policy.getApplyMethod(), "공식 공고 확인"),
                valueOr(policy.getContact(), "공식 공고 확인"),
                detail == null ? null : detail.getBenefit(),
                detail == null ? null : detail.getSelectionCriteria(),
                detail == null ? null : detail.getRequiredDocumentsText(),
                detail == null ? null : detail.getContentText(),
                explanation == null ? null : explanation.getEasySummary(),
                explanation == null ? null : explanation.getEligibilitySummary(),
                explanation == null ? null : explanation.getBenefitSummary(),
                explanation == null ? null : explanation.getApplicationSummary(),
                explanation == null ? null : explanation.getCautionSummary(),
                officialUrl,
                embeddingText
        );
    }

    private String embeddingText(
            Policy policy,
            PolicyDetail detail,
            AiPolicyExplanation explanation,
            String agency,
            String category,
            String region
    ) {
        return """
                정책명: %s
                기관: %s
                분야: %s
                지원 대상: %s
                지역: %s
                신청 기간: %s
                정보 갱신일: %s
                신청 방법: %s
                지원 내용: %s
                선정 기준: %s
                필요 서류: %s
                상세 내용: %s
                검수된 쉬운 설명: %s
                검수된 대상 설명: %s
                검수된 지원 설명: %s
                검수된 신청 설명: %s
                검수된 주의사항: %s
                """.formatted(
                limited(policy.getTitle()),
                limited(agency),
                limited(category),
                limited(policy.getTarget()),
                limited(region),
                limited(applicationPeriod(policy)),
                limited(sourceUpdatedDate(policy)),
                limited(policy.getApplyMethod()),
                limited(detail == null ? null : detail.getBenefit()),
                limited(detail == null ? null : detail.getSelectionCriteria()),
                limited(detail == null ? null : detail.getRequiredDocumentsText()),
                limited(detail == null ? null : detail.getContentText()),
                limited(explanation == null ? null : explanation.getEasySummary()),
                limited(explanation == null ? null : explanation.getEligibilitySummary()),
                limited(explanation == null ? null : explanation.getBenefitSummary()),
                limited(explanation == null ? null : explanation.getApplicationSummary()),
                limited(explanation == null ? null : explanation.getCautionSummary())
        );
    }

    private AiAssistantSourceDto toSource(AiAssistantGroundingDocument document) {
        return new AiAssistantSourceDto(
                document.policyId(),
                document.title(),
                sourceSummary(document),
                document.agency(),
                document.applicationPeriod(),
                document.sourceUpdatedDate(),
                document.officialUrl(),
                "/policies/" + document.policyId()
        );
    }

    private String sourceSummary(AiAssistantGroundingDocument document) {
        String value = firstNonBlank(
                document.approvedEasySummary(),
                document.benefit(),
                document.content(),
                document.target()
        );
        if (!StringUtils.hasText(value)) return "상세 내용은 정책 페이지에서 확인해 주세요.";
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 180) + "…";
    }

    private String retrievalQuestion(
            String question,
            List<AiAssistantMessage> history
    ) {
        String previousUserContext = history.stream()
                .filter(message -> "user".equals(message.role()))
                .map(AiAssistantMessage::content)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
        String combined = (previousUserContext + " " + question).trim();
        return combined.length() <= 1_500
                ? combined
                : combined.substring(combined.length() - 1_500);
    }

    private String applicationPeriod(Policy policy) {
        if (policy.getApplyStatus() == ApplyStatus.ALWAYS) return "상시 신청";
        if (policy.getStartDate() != null && policy.getEndDate() != null) {
            return policy.getStartDate().format(DATE_FORMAT)
                    + " ~ "
                    + policy.getEndDate().format(DATE_FORMAT);
        }
        if (policy.getStartDate() != null) {
            return policy.getStartDate().format(DATE_FORMAT) + "부터";
        }
        if (policy.getEndDate() != null) {
            return policy.getEndDate().format(DATE_FORMAT) + "까지";
        }
        return "공식 공고 확인";
    }

    private String sourceUpdatedDate(Policy policy) {
        if (policy.getUpdatedAt() != null) {
            return policy.getUpdatedAt().toLocalDate().format(DATE_FORMAT);
        }
        if (policy.getCreatedAt() != null) {
            return policy.getCreatedAt().toLocalDate().format(DATE_FORMAT);
        }
        return "확인 필요";
    }

    private String safeOfficialUrl(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            if (scheme == null) return null;
            String normalized = scheme.toLowerCase(Locale.ROOT);
            return "http".equals(normalized) || "https".equals(normalized)
                    ? uri.toString()
                    : null;
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private String limited(String value) {
        if (!StringUtils.hasText(value)) return "정보 없음";
        String normalized = value.trim();
        return normalized.length() <= 3_000
                ? normalized
                : normalized.substring(0, 3_000);
    }

    private String valueOr(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) return value;
        }
        return null;
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of(zoneId));
    }
}
