package com.seoulcareconnect.repository.user;

import com.seoulcareconnect.entity.user.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserId(Long userId);
    Optional<Favorite> findByUserIdAndPolicyId(Long userId, Long policyId);
    void deleteByUserIdAndPolicyId(Long userId, Long policyId);
    boolean existsByUserIdAndPolicyId(Long userId, Long policyId);
}