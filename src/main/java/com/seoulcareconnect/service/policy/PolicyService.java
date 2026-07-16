package com.seoulcareconnect.service.policy;

import com.seoulcareconnect.dto.policy.PolicyDTO;
import com.seoulcareconnect.dto.policy.PolicySearchDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PolicyService {
    Page<PolicyDTO> search(PolicySearchDTO search);
    List<PolicyDTO> closingSoon(int limit);
    List<PolicyDTO> recent(int limit);
    List<PolicyDTO> popular(int limit);
    Page<PolicyDTO> recentViewed(Long userId, int page, int size);
}
