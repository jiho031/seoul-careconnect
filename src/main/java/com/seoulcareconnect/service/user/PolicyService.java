package com.seoulcareconnect.service.user;

import com.seoulcareconnect.entity.user.Policy;
import com.seoulcareconnect.repository.user.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyService {

    private final PolicyRepository policyRepository;

    // 모든 정책 목록을 조회합니다.
    public List<Policy> getAllPolicies() {
        return policyRepository.findAll();
    }
}