package com.seoulcareconnect.repository.policy;

import com.seoulcareconnect.entity.policy.PolicyCollectionError;
import com.seoulcareconnect.entity.policy.enums.PolicyErrorStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyErrorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PolicyCollectionErrorRepository
        extends JpaRepository<PolicyCollectionError, Long>,
        JpaSpecificationExecutor<PolicyCollectionError> {

    long countByStatusNot(PolicyErrorStatus status);

    long countByErrorTypeAndStatusNot(
            PolicyErrorType errorType,
            PolicyErrorStatus status
    );
}