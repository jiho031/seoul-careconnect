package com.seoulcareconnect.dto.admin;

import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.PolicyCollectionError;
import com.seoulcareconnect.entity.policy.PolicyDetail;
import com.seoulcareconnect.entity.policy.enums.PolicyErrorStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyErrorType;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
public class AdminPolicyErrorDTO {

    private final Long errorId;
    private final Long policyId;
    private final Long sourceId;

    private final String externalId;
    private final String policyTitle;
    private final String sourceName;

    private final PolicyErrorType errorType;
    private final String errorTypeLabel;
    private final String errorTypeCssClass;

    private final String errorMessage;

    private final PolicyErrorStatus status;
    private final String statusLabel;
    private final String statusCssClass;

    private final String adminMemo;
    private final LocalDateTime createdAt;
    private final LocalDateTime processedAt;

    /*
     * 모달 직접 수정용 정책 정보
     */
    private final String target;
    private final String region;
    private final String district;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String officialUrl;

    /*
     * 정책 상세 정보
     */
    private final String benefit;
    private final String selectionCriteria;
    private final String requiredDocumentsText;
    private final String contentText;

    private AdminPolicyErrorDTO(
            Long errorId,
            Long policyId,
            Long sourceId,
            String externalId,
            String policyTitle,
            String sourceName,
            PolicyErrorType errorType,
            String errorTypeLabel,
            String errorTypeCssClass,
            String errorMessage,
            PolicyErrorStatus status,
            String statusLabel,
            String statusCssClass,
            String adminMemo,
            LocalDateTime createdAt,
            LocalDateTime processedAt,
            String target,
            String region,
            String district,
            LocalDate startDate,
            LocalDate endDate,
            String officialUrl,
            String benefit,
            String selectionCriteria,
            String requiredDocumentsText,
            String contentText
    ) {
        this.errorId = errorId;
        this.policyId = policyId;
        this.sourceId = sourceId;
        this.externalId = externalId;
        this.policyTitle = policyTitle;
        this.sourceName = sourceName;
        this.errorType = errorType;
        this.errorTypeLabel = errorTypeLabel;
        this.errorTypeCssClass = errorTypeCssClass;
        this.errorMessage = errorMessage;
        this.status = status;
        this.statusLabel = statusLabel;
        this.statusCssClass = statusCssClass;
        this.adminMemo = adminMemo;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.target = target;
        this.region = region;
        this.district = district;
        this.startDate = startDate;
        this.endDate = endDate;
        this.officialUrl = officialUrl;
        this.benefit = benefit;
        this.selectionCriteria = selectionCriteria;
        this.requiredDocumentsText = requiredDocumentsText;
        this.contentText = contentText;
    }

    public static AdminPolicyErrorDTO from(
            PolicyCollectionError error
    ) {
        Policy policy = error.getPolicy();

        Long policyId = policy != null
                ? policy.getPolicyId()
                : null;

        Long sourceId = error.getSource() != null
                ? error.getSource().getSourceId()
                : null;

        String sourceName = error.getSource() != null
                ? error.getSource().getSourceName()
                : "사용자 직접 신고";

        String policyTitle = resolvePolicyTitle(error);
        String externalId = resolveExternalId(error);

        PolicyErrorType errorType = error.getErrorType();
        PolicyErrorStatus status = error.getStatus();

        PolicyDetail detail = policy != null
                ? policy.getDetail()
                : null;

        return new AdminPolicyErrorDTO(
                error.getErrorId(),
                policyId,
                sourceId,
                externalId,
                policyTitle,
                sourceName,

                errorType,
                errorType != null
                        ? errorType.getLabel()
                        : "-",
                getErrorTypeCssClass(errorType),

                error.getErrorMessage(),

                status,
                status != null
                        ? status.getLabel()
                        : "-",
                getStatusCssClass(status),

                error.getAdminMemo(),
                error.getCreatedAt(),
                error.getProcessedAt(),

                policy != null
                        ? policy.getTarget()
                        : null,

                policy != null
                        ? policy.getRegion()
                        : null,

                policy != null
                        ? policy.getDistrict()
                        : null,

                policy != null
                        ? policy.getStartDate()
                        : null,

                policy != null
                        ? policy.getEndDate()
                        : null,

                policy != null
                        ? policy.getOfficialUrl()
                        : null,

                detail != null
                        ? detail.getBenefit()
                        : null,

                detail != null
                        ? detail.getSelectionCriteria()
                        : null,

                detail != null
                        ? detail.getRequiredDocumentsText()
                        : null,

                detail != null
                        ? detail.getContentText()
                        : null
        );
    }

    private static String resolvePolicyTitle(
            PolicyCollectionError error
    ) {
        if (error.getPolicyTitle() != null
                && !error.getPolicyTitle().isBlank()) {

            return error.getPolicyTitle();
        }

        if (error.getPolicy() != null
                && error.getPolicy().getTitle() != null
                && !error.getPolicy().getTitle().isBlank()) {

            return error.getPolicy().getTitle();
        }

        return "정책명 없음";
    }

    private static String resolveExternalId(
            PolicyCollectionError error
    ) {
        if (error.getExternalId() != null
                && !error.getExternalId().isBlank()) {

            return error.getExternalId();
        }

        if (error.getPolicy() != null
                && error.getPolicy().getExternalId() != null
                && !error.getPolicy().getExternalId().isBlank()) {

            return error.getPolicy().getExternalId();
        }

        return "-";
    }

    private static String getErrorTypeCssClass(
            PolicyErrorType errorType
    ) {
        if (errorType == null) {
            return "";
        }

        return switch (errorType) {
            case REQUIRED_VALUE_MISSING -> "missing";
            case DATE_ERROR -> "date";
            case URL_ERROR -> "url";
            case FORMAT_ERROR -> "format";
        };
    }

    private static String getStatusCssClass(
            PolicyErrorStatus status
    ) {
        if (status == null) {
            return "";
        }

        return switch (status) {
            case WAITING -> "wait";
            case IN_PROGRESS -> "ai";
            case COMPLETED -> "ok";
            case EXCLUDED -> "bad";
        };
    }

    public String getCreatedAtText() {
        if (createdAt == null) {
            return "-";
        }

        return createdAt.format(
                DateTimeFormatter.ofPattern(
                        "yyyy.MM.dd HH:mm"
                )
        );
    }

    public String getProcessedAtText() {
        if (processedAt == null) {
            return "-";
        }

        return processedAt.format(
                DateTimeFormatter.ofPattern(
                        "yyyy.MM.dd HH:mm"
                )
        );
    }

    public String getStartDateText() {
        return startDate != null
                ? startDate.toString()
                : "";
    }

    public String getEndDateText() {
        return endDate != null
                ? endDate.toString()
                : "";
    }
}