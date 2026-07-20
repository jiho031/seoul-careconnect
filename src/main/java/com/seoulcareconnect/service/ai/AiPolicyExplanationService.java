package com.seoulcareconnect.service.ai;

import com.seoulcareconnect.dto.ai.AiPolicyExplanationDto;
import com.seoulcareconnect.entity.ai.AiReviewStatus;

import java.util.List;
import java.util.Optional;

public interface AiPolicyExplanationService {

    Optional<AiPolicyExplanationDto> findApproved(Long policyId);

    List<AiPolicyExplanationDto> findRecent(AiReviewStatus status);

    long count(AiReviewStatus status);

    AiPolicyExplanationDto generate(Long policyId);

    AiPolicyExplanationDto regenerate(Long explanationId);

    AiPolicyExplanationDto approve(Long explanationId, String reviewer, String comment);

    AiPolicyExplanationDto reject(Long explanationId, String reviewer, String comment);

    boolean isEnabled();

    String modelName();
}
