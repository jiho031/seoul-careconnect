package com.seoulcareconnect.entity.policy.enums;

public enum PolicyCategory {
    EDUCATION("교육"),
    JOB("일자리"),
    HOUSING("주거"),
    LIVING_SUPPORT("생활지원"),
    CARE("돌봄"),
    CULTURE_LIFE("문화·생활");

    private final String label;

    PolicyCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
