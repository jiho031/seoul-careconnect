package com.seoulcareconnect.repository.ai;

import com.seoulcareconnect.entity.ai.AiPolicyExplanation;
import com.seoulcareconnect.entity.ai.AiPolicyExplanation.ReviewStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface AiPolicyExplanationRepository extends JpaRepository<AiPolicyExplanation, Long> {

    @EntityGraph(attributePaths = "policy")
    Optional<AiPolicyExplanation> findFirstByPolicyPolicyIdAndReviewStatusOrderByCreatedAtDesc(
            Long policyId,
            ReviewStatus reviewStatus
    );

    @EntityGraph(attributePaths = "policy")
    @Query("select explanation from AiPolicyExplanation explanation " +
            "where explanation.explanationId = :explanationId")
    Optional<AiPolicyExplanation> findWithPolicyByExplanationId(
            @Param("explanationId") Long explanationId
    );

    @EntityGraph(attributePaths = "policy")
    List<AiPolicyExplanation> findTop100ByReviewStatusOrderByCreatedAtDesc(ReviewStatus reviewStatus);

    @EntityGraph(attributePaths = "policy")
    List<AiPolicyExplanation> findTop100ByReviewStatusNotOrderByCreatedAtDesc(ReviewStatus reviewStatus);

    List<AiPolicyExplanation> findByPolicyPolicyIdAndReviewStatus(
            Long policyId,
            ReviewStatus reviewStatus
    );

    @EntityGraph(attributePaths = "policy")
    List<AiPolicyExplanation> findByPolicyPolicyIdInAndReviewStatus(
            Collection<Long> policyIds,
            ReviewStatus reviewStatus
    );

    long countByReviewStatus(ReviewStatus reviewStatus);

    long countByReviewStatusAndCreatedAtAfter(
            ReviewStatus reviewStatus,
            LocalDateTime createdAt
    );

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
