package com.seoulcareconnect.service.policy;

import com.seoulcareconnect.dto.policy.PolicyDetailDTO;

public interface PolicyDetailService {
    PolicyDetailDTO get(Long policyId);
}