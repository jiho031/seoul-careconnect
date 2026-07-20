package com.seoulcareconnect.service.impl.user;

import com.seoulcareconnect.dto.user.SearchPresetRequest;
import com.seoulcareconnect.dto.user.SearchPresetResponse;
import com.seoulcareconnect.entity.user.SearchPreset;
import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.user.SearchPresetRepository;
import com.seoulcareconnect.repository.user.UserRepository;
import com.seoulcareconnect.service.user.SearchPresetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchPresetServiceImpl implements SearchPresetService {

    private final SearchPresetRepository searchPresetRepository;
    private final UserRepository userRepository;

    @Override
    public List<SearchPresetResponse> findAll(Long userId) {
        return searchPresetRepository.findAllByUserUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(SearchPresetResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public SearchPresetResponse create(Long userId, SearchPresetRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("로그인 회원을 찾을 수 없습니다."));

        SearchPreset preset = SearchPreset.create(
                user,
                request.getName().trim(),
                trimToNull(request.getKeyword()),
                trimToNull(request.getDistrict()),
                trimToNull(request.getAgeGroup()),
                trimToNull(request.getCategory()),
                trimToNull(request.getTargetKeyword())
        );

        return SearchPresetResponse.from(searchPresetRepository.save(preset));
    }

    @Override
    @Transactional
    public void delete(Long userId, Long presetId) {
        SearchPreset preset = searchPresetRepository.findByPresetIdAndUserUserId(presetId, userId)
                .orElseThrow(() -> new IllegalArgumentException("저장된 프리셋을 찾을 수 없습니다."));

        searchPresetRepository.delete(preset);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
