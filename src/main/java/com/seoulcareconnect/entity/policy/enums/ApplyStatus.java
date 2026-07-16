package com.seoulcareconnect.entity.policy.enums;

public enum ApplyStatus {
    OPEN("신청 가능"),
    CLOSING_SOON("마감 임박"),
    ALWAYS("상시 신청"),
    EXPIRED("마감");

    private final String label;

    ApplyStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
