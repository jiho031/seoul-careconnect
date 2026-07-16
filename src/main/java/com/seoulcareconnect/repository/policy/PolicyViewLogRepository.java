package com.seoulcareconnect.repository.policy;

import com.seoulcareconnect.entity.policy.PolicyViewLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PolicyViewLogRepository extends JpaRepository<PolicyViewLog, Long> {

    @Query("""
            select v.policy.policyId
            from PolicyViewLog v
            where v.user.userId = :userId
            group by v.policy.policyId
            order by max(v.viewedAt) desc
            """)
    List<Long> findRecentPolicyIdsByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );
}