package com.seoulcareconnect.dto.ai;

public record AiAssistantSourceDto(
        Long policyId,
        String title,
        String summary,
        String agency,
        String applicationPeriod,
        String sourceUpdatedDate,
        String officialUrl,
        String detailUrl
) {
}
