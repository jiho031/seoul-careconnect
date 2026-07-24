package com.seoulcareconnect.entity.policy.enums;

public enum PolicyErrorStatus {

    WAITING("확인 대기"),
    IN_PROGRESS("수정 중"),
    COMPLETED("처리 완료"),
    EXCLUDED("제외 처리");

    private final String label;

    PolicyErrorStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}