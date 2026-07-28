package com.seoulcareconnect.dto.ai;

import java.util.List;

public record AiAssistantResponse(
        String answer,
        List<Source> sources,
        boolean grounded,
        String notice
) {
    public record Source(
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
}
