package com.seoulcareconnect.repository.user;

import com.seoulcareconnect.entity.user.PolicyDocumentProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyDocumentProgressRepository
        extends JpaRepository<PolicyDocumentProgress, Long> {

    List<PolicyDocumentProgress> findByUserIdAndPolicyIdAndCompletedTrue(
            Long userId,
            Long policyId
    );

    Optional<PolicyDocumentProgress> findByUserIdAndPolicyIdAndDocumentKey(
            Long userId,
            Long policyId,
            String documentKey
    );
}
