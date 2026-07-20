package com.seoulcareconnect.dto.ai;

import com.seoulcareconnect.entity.ai.AiReviewStatus;

import java.time.LocalDateTime;

public record AiPolicyExplanationDto(
        Long explanationId,
        Long policyId,
        String policyTitle,
        AiReviewStatus reviewStatus,
        String reviewStatusLabel,
        String easySummary,
        String eligibilitySummary,
        String benefitSummary,
        String applicationSummary,
        String cautionSummary,
        String modelName,
        String promptVersion,
        String reviewComment,
        String reviewedBy,
        LocalDateTime sourceUpdatedAt,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {
}
