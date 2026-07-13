package com.seoulcareconnect.repository.policy;

import com.seoulcareconnect.entity.policy.RawCollectedItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RawCollectedItemRepository extends JpaRepository<RawCollectedItem, Long> {

    Optional<RawCollectedItem> findFirstBySource_SourceIdAndContentHashOrderByCollectedAtDesc(
            Long sourceId,
            String contentHash
    );
}
