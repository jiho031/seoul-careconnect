package com.seoulcareconnect.service.impl.ai;

import com.seoulcareconnect.config.ai.AiProperties;
import com.seoulcareconnect.dto.ai.AiPolicyExplanationContent;
import com.seoulcareconnect.dto.ai.AiPolicyExplanationDto;
import com.seoulcareconnect.entity.ai.AiPolicyExplanation;
import com.seoulcareconnect.entity.ai.AiReviewStatus;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.repository.ai.AiPolicyExplanationRepository;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import com.seoulcareconnect.service.ai.AiPolicyExplanationService;
import com.seoulcareconnect.service.ai.PolicyExplanationGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiPolicyExplanationServiceImpl implements AiPolicyExplanationService {

    private final AiPolicyExplanationRepository explanationRepository;
    private final PolicyRepository policyRepository;
    private final PolicyExplanationGenerator generator;
    private final AiProperties properties;

    @Override
    public Optional<AiPolicyExplanationDto> findApproved(Long policyId) {
        return explanationRepository
                .findFirstByPolicyPolicyIdAndReviewStatusOrderByCreatedAtDesc(
                        policyId,
                        AiReviewStatus.APPROVED
                )
                .map(this::toDto);
    }

    @Override
    public List<AiPolicyExplanationDto> findRecent(AiReviewStatus status) {
        List<AiPolicyExplanation> explanations = status == null
                ? explanationRepository.findTop100ByReviewStatusNotOrderByCreatedAtDesc(
                        AiReviewStatus.SUPERSEDED
                )
                : explanationRepository.findTop100ByReviewStatusOrderByCreatedAtDesc(status);

        return explanations.stream().map(this::toDto).toList();
    }

    @Override
    public long count(AiReviewStatus status) {
        return explanationRepository.countByReviewStatus(status);
    }

    @Override
    @Transactional
    public AiPolicyExplanationDto generate(Long policyId) {
        Policy policy = policyRepository.findWithSourceAndDetailByPolicyId(policyId)
                .orElseThrow(() -> new IllegalArgumentException("정책을 찾을 수 없습니다. ID=" + policyId));

        supersedeExistingDrafts(policyId);
        return createDraft(policy);
    }

    @Override
    @Transactional
    public AiPolicyExplanationDto regenerate(Long explanationId) {
        AiPolicyExplanation current = findExplanation(explanationId);
        Policy policy = policyRepository.findWithSourceAndDetailByPolicyId(current.getPolicy().getPolicyId())
                .orElseThrow(() -> new IllegalArgumentException("정책을 찾을 수 없습니다."));

        if (current.getReviewStatus() != AiReviewStatus.APPROVED) {
            current.supersede();
        }
        supersedeExistingDrafts(policy.getPolicyId());
        return createDraft(policy);
    }

    @Override
    @Transactional
    public AiPolicyExplanationDto approve(Long explanationId, String reviewer, String comment) {
        AiPolicyExplanation target = findExplanation(explanationId);
        if (target.getReviewStatus() != AiReviewStatus.DRAFT) {
            throw new IllegalStateException("검수 대기 상태의 설명만 승인할 수 있습니다.");
        }

        explanationRepository.findByPolicyPolicyIdAndReviewStatus(
                        target.getPolicy().getPolicyId(),
                        AiReviewStatus.APPROVED
                )
                .stream()
                .filter(existing -> !existing.getExplanationId().equals(target.getExplanationId()))
                .forEach(AiPolicyExplanation::supersede);

        target.approve(reviewer, comment);
        return toDto(target);
    }

    @Override
    @Transactional
    public AiPolicyExplanationDto reject(Long explanationId, String reviewer, String comment) {
        AiPolicyExplanation target = findExplanation(explanationId);
        if (target.getReviewStatus() != AiReviewStatus.DRAFT) {
            throw new IllegalStateException("검수 대기 상태의 설명만 반려할 수 있습니다.");
        }
        target.reject(reviewer, comment);
        return toDto(target);
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public String modelName() {
        return properties.getModel();
    }

    private AiPolicyExplanationDto createDraft(Policy policy) {
        AiPolicyExplanationContent content = generator.generate(policy);
        AiPolicyExplanation saved = explanationRepository.save(
                AiPolicyExplanation.draft(
                        policy,
                        content,
                        properties.getModel(),
                        properties.getPromptVersion()
                )
        );
        return toDto(saved);
    }

    private void supersedeExistingDrafts(Long policyId) {
        explanationRepository.findByPolicyPolicyIdAndReviewStatus(
                        policyId,
                        AiReviewStatus.DRAFT
                )
                .forEach(AiPolicyExplanation::supersede);
    }

    private AiPolicyExplanation findExplanation(Long explanationId) {
        return explanationRepository.findWithPolicyByExplanationId(explanationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "AI 정책 설명을 찾을 수 없습니다. ID=" + explanationId
                ));
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
}
