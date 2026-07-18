package com.seoulcareconnect.repository.user;

import com.seoulcareconnect.entity.user.SearchPreset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SearchPresetRepository extends JpaRepository<SearchPreset, Long> {

    List<SearchPreset> findAllByUserUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<SearchPreset> findByPresetIdAndUserUserId(Long presetId, Long userId);
}
