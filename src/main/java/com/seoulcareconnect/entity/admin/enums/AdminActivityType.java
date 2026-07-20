package com.seoulcareconnect.entity.admin.enums;

public enum AdminActivityType {

    POLICY_CREATE("정책 등록"),
    POLICY_UPDATE("정책 수정"),
    POLICY_APPROVE("정책 승인"),
    POLICY_REJECT("정책 반려"),
    POLICY_DELETE("정책 삭제"),

    COLLECTION_RUN("API 수집"),

    NOTICE_CREATE("관리자 공지 등록"),
    NOTICE_UPDATE("관리자 공지 수정"),
    NOTICE_DELETE("관리자 공지 삭제"),

    USER_STATUS_UPDATE("회원 상태 변경"),
    ADMIN_ROLE_APPROVE("관리자 권한 승인"),
    ADMIN_ROLE_REJECT("관리자 권한 거절"),

    REPORT_PROCESS("사용자 신고 처리"),
    AI_SUMMARY_APPROVE("AI 요약 승인"),
    AI_SUMMARY_REJECT("AI 요약 재검토"),

    OTHER("기타 관리자 활동");

    private final String label;

    AdminActivityType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}