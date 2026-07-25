package com.seoulcareconnect.dto.ai;

import java.util.List;

public record AiAssistantResponse(
        String answer,
        List<AiAssistantSourceDto> sources,
        boolean grounded,
        String notice
) {
}
