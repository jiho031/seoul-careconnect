package com.seoulcareconnect.repository.policy;

import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.PolicyDocument;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, Long> {

    @EntityGraph(attributePaths = "guide")
    List<PolicyDocument> findByPolicy_PolicyIdOrderBySortOrderAscPolicyDocumentIdAsc(
            Long policyId
    );

    Optional<PolicyDocument> findByPolicyDocumentIdAndPolicy_PolicyId(
            Long policyDocumentId,
            Long policyId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PolicyDocument document where document.policy.policyId = :policyId")
    void deleteAllByPolicyId(@Param("policyId") Long policyId);

    @Query("""
            select policy
            from Policy policy
            join fetch policy.detail detail
            where detail.requiredDocumentsText is not null
              and not exists (
                  select document.policyDocumentId
                  from PolicyDocument document
                  where document.policy = policy
              )
            order by policy.policyId
            """)
    List<Policy> findPoliciesNeedingDocumentBackfill();
}
