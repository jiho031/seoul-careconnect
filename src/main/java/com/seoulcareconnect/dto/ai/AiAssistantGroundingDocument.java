package com.seoulcareconnect.dto.ai;

public record AiAssistantGroundingDocument(
        int citationNumber,
        Long policyId,
        String title,
        String agency,
        String category,
        String target,
        String region,
        String applicationPeriod,
        String sourceUpdatedDate,
        String applicationMethod,
        String contact,
        String benefit,
        String selectionCriteria,
        String requiredDocuments,
        String content,
        String approvedEasySummary,
        String approvedEligibilitySummary,
        String approvedBenefitSummary,
        String approvedApplicationSummary,
        String approvedCautionSummary,
        String officialUrl,
        String embeddingText
) {
}
