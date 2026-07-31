package com.seoulcareconnect.entity.policy.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentCategory {
    RESIDENT_FAMILY("주민·가족관계"),
    INCOME_TAX("소득·세금"),
    SOCIAL_INSURANCE("건강·사회보험"),
    EMPLOYMENT_CAREER("취업·경력"),
    BUSINESS_CORPORATE("사업·법인"),
    HOUSING_PROPERTY("주거·재산"),
    EDUCATION_QUALIFICATION("교육·자격"),
    WELFARE_ELIGIBILITY("복지자격"),
    FINANCE_PAYMENT("금융·지급"),
    ANNOUNCEMENT_FORM("공고 전용 양식"),
    SELF_WRITTEN("직접 작성 자료"),
    GENERAL_EVIDENCE("포괄·조건부 증빙"),
    OTHER("기타 서류");

    private final String label;
}
