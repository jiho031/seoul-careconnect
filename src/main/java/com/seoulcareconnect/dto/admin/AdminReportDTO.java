package com.seoulcareconnect.dto.admin;

import com.seoulcareconnect.entity.report.MissingPolicyReport;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
public class AdminReportDTO {

    private final Long reportId;
    private final Long userId;
    private final Long policyId;

    private final String reporterName;
    private final String reporterEmail;

    private final String title;
    private final String policyTitle;
    private final String sourceUrl;

    private final String reportType;
    private final String reportTypeLabel;
    private final String reportTypeCssClass;

    private final String content;

    private final String status;
    private final String statusLabel;
    private final String statusCssClass;

    private final String adminMemo;

    private final LocalDateTime createdAt;
    private final LocalDateTime processedAt;

    private AdminReportDTO(
            Long reportId,
            Long userId,
            Long policyId,
            String reporterName,
            String reporterEmail,
            String title,
            String policyTitle,
            String sourceUrl,
            String reportType,
            String reportTypeLabel,
            String reportTypeCssClass,
            String content,
            String status,
            String statusLabel,
            String statusCssClass,
            String adminMemo,
            LocalDateTime createdAt,
            LocalDateTime processedAt
    ) {
        this.reportId = reportId;
        this.userId = userId;
        this.policyId = policyId;
        this.reporterName = reporterName;
        this.reporterEmail = reporterEmail;
        this.title = title;
        this.policyTitle = policyTitle;
        this.sourceUrl = sourceUrl;
        this.reportType = reportType;
        this.reportTypeLabel = reportTypeLabel;
        this.reportTypeCssClass = reportTypeCssClass;
        this.content = content;
        this.status = status;
        this.statusLabel = statusLabel;
        this.statusCssClass = statusCssClass;
        this.adminMemo = adminMemo;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    public static AdminReportDTO from(MissingPolicyReport report) {
        Long userId = report.getUser() != null
                ? report.getUser().getUserId()
                : null;

        Long policyId = report.getPolicy() != null
                ? report.getPolicy().getPolicyId()
                : null;

        /*
         * User 엔티티의 실제 이름·이메일 getter가 다르면
         * 아래 두 줄만 실제 필드명에 맞게 수정하면 된다.
         */
        String reporterName = report.getUser() != null
                ? report.getUser().getName()
                : "비회원";

        String reporterEmail = report.getUser() != null
                ? report.getUser().getEmail()
                : "-";

        String policyTitle = report.getPolicy() != null
                && report.getPolicy().getTitle() != null
                ? report.getPolicy().getTitle()
                : "누락 정책 제보";

        return new AdminReportDTO(
                report.getReportId(),
                userId,
                policyId,
                reporterName,
                reporterEmail,
                report.getTitle(),
                policyTitle,
                report.getSourceUrl(),
                report.getReportType(),
                getReportTypeLabel(report.getReportType()),
                getReportTypeCssClass(report.getReportType()),
                report.getContent(),
                report.getStatus(),
                getStatusLabel(report.getStatus()),
                getStatusCssClass(report.getStatus()),
                report.getAdminMemo(),
                report.getCreatedAt(),
                report.getProcessedAt()
        );
    }

    private static String getReportTypeLabel(String reportType) {
        if (reportType == null) {
            return "-";
        }

        return switch (reportType) {
            case "MISSING_POLICY" -> "누락 정책";
            case "INFO_ERROR" -> "정보 오류";
            case "DEADLINE_ERROR" -> "기간 오류";
            case "LINK_ERROR" -> "링크 오류";
            default -> reportType;
        };
    }

    private static String getReportTypeCssClass(String reportType) {
        if (reportType == null) {
            return "";
        }

        return switch (reportType) {
            case "MISSING_POLICY" -> "missing";
            case "INFO_ERROR" -> "info";
            case "DEADLINE_ERROR" -> "date";
            case "LINK_ERROR" -> "url";
            default -> "";
        };
    }

    private static String getStatusLabel(String status) {
        if (status == null) {
            return "-";
        }

        return switch (status) {
            case "RECEIVED" -> "접수";
            case "IN_REVIEW" -> "검토 중";
            case "COMPLETED" -> "처리 완료";
            case "REJECTED" -> "반려";
            default -> status;
        };
    }

    private static String getStatusCssClass(String status) {
        if (status == null) {
            return "";
        }

        return switch (status) {
            case "RECEIVED" -> "wait";
            case "IN_REVIEW" -> "ai";
            case "COMPLETED" -> "ok";
            case "REJECTED" -> "bad";
            default -> "";
        };
    }

    public String getCreatedAtText() {
        return formatDateTime(createdAt);
    }

    public String getProcessedAtText() {
        return formatDateTime(processedAt);
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }

        return dateTime.format(
                DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
        );
    }
}