package com.seoulcareconnect.repository.policy;

import com.seoulcareconnect.entity.policy.PolicyDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface PolicyDetailRepository
        extends JpaRepository<PolicyDetail, Long> {

    Optional<PolicyDetail> findByPolicy_PolicyId(
            Long policyId
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            delete from PolicyDetail d
            where d.policy.policyId in :policyIds
            """)
    int deleteByPolicyIds(
            @Param("policyIds")
            Collection<Long> policyIds
    );
}