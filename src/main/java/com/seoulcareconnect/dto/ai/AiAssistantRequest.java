package com.seoulcareconnect.dto.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AiAssistantRequest(
        @NotBlank
        @Size(max = 500)
        String question,

        @Positive
        Long policyId,

        @Valid
        @Size(max = 6)
        List<AiAssistantMessage> history
) {
    public List<AiAssistantMessage> safeHistory() {
        return history == null ? List.of() : List.copyOf(history);
    }
}
