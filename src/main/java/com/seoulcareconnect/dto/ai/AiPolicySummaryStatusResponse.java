package com.seoulcareconnect.dto.ai;

public record AiPolicySummaryStatusResponse(
        Long policyId,
        String state,
        String stageLabel,
        int stage,
        int totalStages,
        String message
) {
}
