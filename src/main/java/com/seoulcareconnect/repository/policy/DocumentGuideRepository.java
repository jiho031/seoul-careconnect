package com.seoulcareconnect.repository.policy;

import com.seoulcareconnect.entity.policy.DocumentGuide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentGuideRepository extends JpaRepository<DocumentGuide, Long> {
    Optional<DocumentGuide> findByGuideKey(String guideKey);

    List<DocumentGuide> findByActiveTrueOrderByDisplayNameAsc();
}
