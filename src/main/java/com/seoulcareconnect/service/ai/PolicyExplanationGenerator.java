package com.seoulcareconnect.service.ai;

import com.seoulcareconnect.dto.ai.AiPolicyExplanationContent;
import com.seoulcareconnect.entity.policy.Policy;

public interface PolicyExplanationGenerator {

    AiPolicyExplanationContent generate(Policy policy);
}
