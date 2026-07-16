package com.seoulcareconnect.repository.policy;

import com.seoulcareconnect.entity.policy.PolicyDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolicyDetailRepository extends JpaRepository<PolicyDetail, Long> {
    Optional<PolicyDetail> findByPolicy_PolicyId(Long policyId);
}
