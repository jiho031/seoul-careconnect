package com.seoulcareconnect.repository.ai;

import com.seoulcareconnect.entity.ai.AiPolicyExplanation;
import com.seoulcareconnect.entity.ai.AiPolicyExplanation.ReviewStatus;
import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
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
    @Query("""
            select explanation
            from AiPolicyExplanation explanation
            join explanation.policy policy
            where explanation.reviewStatus = :reviewStatus
              and policy.applyStatus <> :expiredApplyStatus
              and (policy.endDate is null or policy.endDate >= :today)
            order by explanation.createdAt desc
            """)
    Page<AiPolicyExplanation> findActiveGeneratedPage(
            @Param("reviewStatus") ReviewStatus reviewStatus,
            @Param("expiredApplyStatus") ApplyStatus expiredApplyStatus,
            @Param("today") LocalDate today,
            Pageable pageable
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

    @Query("""
            select count(explanation)
            from AiPolicyExplanation explanation
            join explanation.policy policy
            where explanation.reviewStatus = :reviewStatus
              and policy.applyStatus <> :expiredApplyStatus
              and (policy.endDate is null or policy.endDate >= :today)
            """)
    long countActiveGenerated(
            @Param("reviewStatus") ReviewStatus reviewStatus,
            @Param("expiredApplyStatus") ApplyStatus expiredApplyStatus,
            @Param("today") LocalDate today
    );

    @Query("""
            select count(explanation)
            from AiPolicyExplanation explanation
            join explanation.policy policy
            where explanation.reviewStatus = :reviewStatus
              and policy.applyStatus <> :expiredApplyStatus
              and (policy.endDate is null or policy.endDate >= :today)
              and explanation.createdAt >= :createdAt
            """)
    long countActiveGeneratedSince(
            @Param("reviewStatus") ReviewStatus reviewStatus,
            @Param("expiredApplyStatus") ApplyStatus expiredApplyStatus,
            @Param("today") LocalDate today,
            @Param("createdAt") LocalDateTime createdAt
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