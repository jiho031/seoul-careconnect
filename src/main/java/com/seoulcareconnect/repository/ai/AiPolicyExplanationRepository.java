package com.seoulcareconnect.repository.ai;

import com.seoulcareconnect.entity.ai.AiPolicyExplanation;
import com.seoulcareconnect.entity.ai.AiReviewStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface AiPolicyExplanationRepository extends JpaRepository<AiPolicyExplanation, Long> {

    @EntityGraph(attributePaths = "policy")
    Optional<AiPolicyExplanation> findFirstByPolicyPolicyIdAndReviewStatusOrderByCreatedAtDesc(
            Long policyId,
            AiReviewStatus reviewStatus
    );

    @EntityGraph(attributePaths = "policy")
    @Query("select explanation from AiPolicyExplanation explanation " +
            "where explanation.explanationId = :explanationId")
    Optional<AiPolicyExplanation> findWithPolicyByExplanationId(
            @Param("explanationId") Long explanationId
    );

    @EntityGraph(attributePaths = "policy")
    List<AiPolicyExplanation> findTop100ByReviewStatusOrderByCreatedAtDesc(AiReviewStatus reviewStatus);

    @EntityGraph(attributePaths = "policy")
    List<AiPolicyExplanation> findTop100ByReviewStatusNotOrderByCreatedAtDesc(AiReviewStatus reviewStatus);

    List<AiPolicyExplanation> findByPolicyPolicyIdAndReviewStatus(
            Long policyId,
            AiReviewStatus reviewStatus
    );

    long countByReviewStatus(AiReviewStatus reviewStatus);

    @Modifying(flushAutomatically = true)
    @Query("""
        delete from AiPolicyExplanation a
        where a.policy.policyId in :policyIds
        """)
    int deleteByPolicyIds(
            @Param("policyIds")
            Collection<Long> policyIds
    );
}
