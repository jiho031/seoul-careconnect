package com.seoulcareconnect.dto.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import jakarta.validation.constraints.Pattern;

public record AiAssistantRequest(
        @NotBlank
        @Size(max = 500)
        String question,

        @Positive
        Long policyId,

        @Valid
        @Size(max = 6)
        List<Message> history
) {
    public List<Message> safeHistory() {
        return history == null ? List.of() : List.copyOf(history);
    }

    public record Message(
            @NotBlank
            @Pattern(regexp = "user|assistant")
            String role,

            @NotBlank
            @Size(max = 4_000)
            String content
    ) {
    }

}
