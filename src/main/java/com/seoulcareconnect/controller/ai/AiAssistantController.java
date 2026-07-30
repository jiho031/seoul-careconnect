package com.seoulcareconnect.controller.ai;

import com.seoulcareconnect.dto.ai.AiAssistantRequest;
import com.seoulcareconnect.dto.ai.AiAssistantResponse;
import com.seoulcareconnect.service.ai.AiPolicyAssistantService;
import com.seoulcareconnect.service.ai.AiModelGateway;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/assistant")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiPolicyAssistantService assistantService;

    @PostMapping
    public AiAssistantResponse ask(
            @Valid @RequestBody AiAssistantRequest request,
            Authentication authentication
    ) {
        return assistantService.ask(request, authentication);
    }

    @PostMapping("/policies/{policyId}/summary")
    public AiAssistantResponse initializePolicy(
            @PathVariable Long policyId
    ) {
        return assistantService.initializePolicy(policyId);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<java.util.Map<String, String>> handleValidation() {
        return ResponseEntity.badRequest().body(java.util.Map.of(
                "message",
                "질문은 1자 이상 500자 이하로 입력해 주세요."
        ));
    }

    @ExceptionHandler(AiModelGateway.UnavailableException.class)
    public ResponseEntity<java.util.Map<String, String>> handleUnavailable(
            AiModelGateway.UnavailableException exception
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(java.util.Map.of(
                "message",
                exception.getMessage()
        ));
    }
}
