package com.seoulcareconnect.repository.policy;

import com.seoulcareconnect.entity.policy.PolicySource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicySourceRepository extends JpaRepository<PolicySource, Long> {
    Optional<PolicySource> findFirstBySourceName(String sourceName);

    List<PolicySource> findAllByIsActiveTrueOrderBySourceNameAsc();
}
