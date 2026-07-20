package com.seoulcareconnect.service.user;

import com.seoulcareconnect.dto.user.SearchPresetRequest;
import com.seoulcareconnect.dto.user.SearchPresetResponse;

import java.util.List;

public interface SearchPresetService {

    List<SearchPresetResponse> findAll(Long userId);

    SearchPresetResponse create(Long userId, SearchPresetRequest request);

    void delete(Long userId, Long presetId);
}
