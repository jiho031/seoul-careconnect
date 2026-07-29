package com.seoulcareconnect.dto.admin;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class AdminPolicyExceptionDTO {

    private Long validationId;

    private Long policyId;
    private Long rawId;

    private String policyTitle;
    private String sourceName;

    private boolean requiredFieldsOk;
    private boolean dateValid;
    private boolean duplicateSuspect;
    private boolean urlValid;
    private boolean categoryClassified;
    private boolean targetDetected;
    private boolean aiProcessed;
    private boolean autoPublished;
    private boolean reviewRequired;

    private String reviewReason;
    private LocalDateTime validatedAt;

    public String getExceptionTypeLabel() {
        if (!requiredFieldsOk) {
            return "필수값 누락";
        }

        if (!dateValid) {
            return "기간 오류";
        }

        if (duplicateSuspect) {
            return "중복 후보";
        }

        if (!urlValid) {
            return "URL 오류";
        }

        if (!categoryClassified) {
            return "카테고리 분류 실패";
        }

        if (!targetDetected) {
            return "대상 정보 부족";
        }

        if (!aiProcessed) {
            return "AI 처리 실패";
        }

        return "기타 예외";
    }

    public String getExceptionCssClass() {
        if (duplicateSuspect) {
            return "duplicate";
        }

        if (!requiredFieldsOk || !dateValid || !urlValid) {
            return "danger";
        }

        if (!categoryClassified || !targetDetected || !aiProcessed) {
            return "warning";
        }

        return "wait";
    }

    public String getValidatedAtText() {
        if (validatedAt == null) {
            return "-";
        }

        return validatedAt.format(
                DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
        );
    }

    public String getSafeReviewReason() {
        if (reviewReason == null || reviewReason.isBlank()) {
            return "검수 사유가 등록되지 않았습니다.";
        }

        return reviewReason;
    }
}