package com.seoulcareconnect.entity.policy.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentRequirementType {
    REQUIRED("필수"),
    CONDITIONAL("해당하는 경우"),
    OPTIONAL("필요한 경우"),
    ALTERNATIVE("둘 중 하나");

    private final String label;
}
