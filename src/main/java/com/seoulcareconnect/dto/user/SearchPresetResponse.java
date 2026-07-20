package com.seoulcareconnect.dto.user;

import com.seoulcareconnect.entity.user.SearchPreset;

public record SearchPresetResponse(
        Long presetId,
        String name,
        String keyword,
        String district,
        String ageGroup,
        String category,
        String targetKeyword
) {

    public static SearchPresetResponse from(SearchPreset preset) {
        return new SearchPresetResponse(
                preset.getPresetId(),
                preset.getName(),
                preset.getKeyword(),
                preset.getDistrict(),
                preset.getAgeGroup(),
                preset.getCategory(),
                preset.getTargetKeyword()
        );
    }
}
