package com.seoulcareconnect.entity.policy.enums;

public enum PolicyErrorType {

    REQUIRED_VALUE_MISSING("필수값 누락"),
    DATE_ERROR("기간 오류"),
    URL_ERROR("URL 오류"),
    FORMAT_ERROR("형식 오류");

    private final String label;

    PolicyErrorType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}