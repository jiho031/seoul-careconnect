package com.seoulcareconnect.repository.user;

import com.seoulcareconnect.entity.user.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserId(Long userId);

    Optional<Favorite> findByUserIdAndPolicyId(
            Long userId,
            Long policyId
    );

    void deleteByUserIdAndPolicyId(
            Long userId,
            Long policyId
    );

    boolean existsByUserIdAndPolicyId(
            Long userId,
            Long policyId
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            delete from Favorite f
            where f.policyId in :policyIds
            """)
    int deleteByPolicyIds(
            @Param("policyIds")
            Collection<Long> policyIds
    );
}