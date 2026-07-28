package com.seoulcareconnect.repository.policy;

import com.seoulcareconnect.entity.policy.PolicyCollectionError;
import com.seoulcareconnect.entity.policy.enums.PolicyErrorStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyErrorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface PolicyCollectionErrorRepository
        extends JpaRepository<PolicyCollectionError, Long>,
        JpaSpecificationExecutor<PolicyCollectionError> {

    long countByStatusNot(
            PolicyErrorStatus status
    );

    long countByErrorTypeAndStatusNot(
            PolicyErrorType errorType,
            PolicyErrorStatus status
    );

    @Modifying(flushAutomatically = true)
    @Query("""
        update PolicyCollectionError e
           set e.policy = null
         where e.policy.policyId in :policyIds
        """)
    int clearPolicyReferences(
            @Param("policyIds")
            Collection<Long> policyIds
    );

    Optional<PolicyCollectionError>
    findByReport_ReportId(
            Long reportId
    );
}