package com.seoulcareconnect.controller.ai;

import com.seoulcareconnect.dto.ai.AiAssistantRequest;
import com.seoulcareconnect.dto.ai.AiAssistantResponse;
import com.seoulcareconnect.service.ai.AiPolicyAssistantService;
import com.seoulcareconnect.service.ai.AiAssistantUserContextResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/assistant")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiPolicyAssistantService assistantService;
    private final AiAssistantUserContextResolver userContextResolver;

    @PostMapping
    public AiAssistantResponse ask(
            @Valid @RequestBody AiAssistantRequest request,
            Authentication authentication
    ) {
        return assistantService.ask(
                request,
                userContextResolver.resolve(authentication)
        );
    }
}
