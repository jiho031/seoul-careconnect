package com.seoulcareconnect.entity.policy.enums;

public enum AgeGroup {
    UNDER_40("30대 이하"),
    FORTIES("40대"),
    FIFTIES("50대"),
    SIXTIES_PLUS("60대 이상"),
    ALL("전 연령");

    private final String label;

    AgeGroup(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}