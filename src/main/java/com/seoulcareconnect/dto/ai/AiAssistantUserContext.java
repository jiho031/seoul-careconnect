package com.seoulcareconnect.dto.ai;

import org.springframework.util.StringUtils;

public record AiAssistantUserContext(
        String district,
        String ageGroup
) {
    public static AiAssistantUserContext empty() {
        return new AiAssistantUserContext(null, null);
    }

    public boolean hasDistrict() {
        return StringUtils.hasText(district);
    }

    public boolean hasAgeGroup() {
        return StringUtils.hasText(ageGroup);
    }
}
