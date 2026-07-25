package com.seoulcareconnect.service.impl.ai;

import com.seoulcareconnect.dto.ai.AiAssistantGroundingDocument;
import com.seoulcareconnect.entity.policy.Policy;

record AiAssistantCandidate(
        Policy policy,
        AiAssistantGroundingDocument document,
        int lexicalScore
) {
}
