package com.seoulcareconnect.repository.policy;

import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long>, JpaSpecificationExecutor<Policy> {

    Optional<Policy> findFirstBySource_SourceIdAndExternalId(Long sourceId, String externalId);

    @EntityGraph(attributePaths = {"source", "detail"})
    Optional<Policy> findWithSourceAndDetailByPolicyId(Long policyId);

    @EntityGraph(attributePaths = {"source", "detail"})
    List<Policy> findAllByPolicyIdInAndStatusInAndApplyStatusNot(
            Collection<Long> policyIds,
            Collection<PolicyStatus> statuses,
            ApplyStatus excludedApplyStatus
    );

    @EntityGraph(attributePaths = {"source", "detail"})
    @Query("""
        select p
        from Policy p
        where p.status in :statuses
          and p.applyStatus <> :expired
          and (p.startDate is null or p.startDate <= :today)
          and p.endDate between :today and :closingDate
        order by p.endDate asc, p.createdAt desc
        """)
    List<Policy> findClosingSoon(
            @Param("statuses") Collection<PolicyStatus> statuses,
            @Param("expired") ApplyStatus expired,
            @Param("today") LocalDate today,
            @Param("closingDate") LocalDate closingDate,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"source", "detail"})
    @Query("""
            select p
            from Policy p
            where p.status in :statuses
              and p.applyStatus <> :expired
              and (p.startDate is null or p.startDate <= :today)
              and (p.endDate is null or p.endDate >= :today)
            order by p.createdAt desc
            """)
    List<Policy> findRecent(
            @Param("statuses") Collection<PolicyStatus> statuses,
            @Param("expired") ApplyStatus expired,
            @Param("today") LocalDate today,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"source", "detail"})
    @Query("""
            select p
            from Policy p
            where p.status in :statuses
              and p.applyStatus <> :expired
              and (p.startDate is null or p.startDate <= :today)
              and (p.endDate is null or p.endDate >= :today)
            order by p.viewCount desc, p.createdAt desc
            """)
    List<Policy> findPopular(
            @Param("statuses") Collection<PolicyStatus> statuses,
            @Param("expired") ApplyStatus expired,
            @Param("today") LocalDate today,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Policy p
               set p.viewCount = p.viewCount + 1
             where p.policyId = :policyId
            """)
    int increaseViewCount(@Param("policyId") Long policyId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Policy p
               set p.applyStatus = com.seoulcareconnect.entity.policy.enums.ApplyStatus.OPEN,
                   p.updatedAt = :now
             where p.endDate > :closingDate
               and p.applyStatus = com.seoulcareconnect.entity.policy.enums.ApplyStatus.CLOSING_SOON
            """)
    int reopenPoliciesOutsideClosingWindow(
            @Param("closingDate") LocalDate closingDate,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Policy p
               set p.applyStatus = com.seoulcareconnect.entity.policy.enums.ApplyStatus.CLOSING_SOON,
                   p.updatedAt = :now
             where p.endDate between :today and :closingDate
               and p.applyStatus = com.seoulcareconnect.entity.policy.enums.ApplyStatus.OPEN
            """)
    int markClosingSoonPolicies(
            @Param("today") LocalDate today,
            @Param("closingDate") LocalDate closingDate,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Policy p
               set p.applyStatus = com.seoulcareconnect.entity.policy.enums.ApplyStatus.EXPIRED,
                   p.status = com.seoulcareconnect.entity.policy.enums.PolicyStatus.EXPIRED,
                   p.updatedAt = :now
             where p.endDate < :today
               and p.applyStatus <> com.seoulcareconnect.entity.policy.enums.ApplyStatus.EXPIRED
               and p.status <> com.seoulcareconnect.entity.policy.enums.PolicyStatus.HIDDEN
            """)
    int expireEndedPolicies(
            @Param("today") LocalDate today,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Policy p
               set p.status = com.seoulcareconnect.entity.policy.enums.PolicyStatus.HIDDEN,
                   p.updatedAt = :now
             where p.endDate <= :hideBefore
               and p.applyStatus = com.seoulcareconnect.entity.policy.enums.ApplyStatus.EXPIRED
               and p.status = com.seoulcareconnect.entity.policy.enums.PolicyStatus.EXPIRED
            """)
    int hideOldExpiredPolicies(
            @Param("hideBefore") LocalDate hideBefore,
            @Param("now") LocalDateTime now
    );
}
