package com.seoulcareconnect.service.ai;

import com.seoulcareconnect.dto.ai.AiAssistantRequest;
import com.seoulcareconnect.dto.ai.AiAssistantResponse;
import com.seoulcareconnect.dto.ai.AiAssistantUserContext;

public interface AiPolicyAssistantService {

    AiAssistantResponse ask(
            AiAssistantRequest request,
            AiAssistantUserContext userContext
    );
}
