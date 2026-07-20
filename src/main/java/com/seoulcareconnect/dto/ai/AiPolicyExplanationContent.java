package com.seoulcareconnect.dto.ai;

public record AiPolicyExplanationContent(
        String easySummary,
        String eligibilitySummary,
        String benefitSummary,
        String applicationSummary,
        String cautionSummary
) {
}
