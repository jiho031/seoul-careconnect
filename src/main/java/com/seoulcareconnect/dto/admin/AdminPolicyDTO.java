package com.seoulcareconnect.dto.admin;

import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.entity.policy.enums.PolicyStatus;
import lombok.Getter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

@Getter
public class AdminPolicyDTO {

    private final Long policyId;
    private final String externalId;

    private final String title;
    private final String organization;
    private final String sourceName;

    private final PolicyCategory category;
    private final String categoryLabel;

    private final String target;
    private final String region;
    private final String district;

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String applicationPeriodText;

    private final ApplyStatus applyStatus;
    private final String applyStatusLabel;

    private final PolicyStatus status;
    private final String statusLabel;
    private final String statusCssClass;

    private final String sourceTypeLabel;
    private final String sourceCssClass;

    private final Integer viewCount;

    private AdminPolicyDTO(
            Long policyId,
            String externalId,
            String title,
            String organization,
            String sourceName,
            PolicyCategory category,
            String categoryLabel,
            String target,
            String region,
            String district,
            LocalDate startDate,
            LocalDate endDate,
            String applicationPeriodText,
            ApplyStatus applyStatus,
            String applyStatusLabel,
            PolicyStatus status,
            String statusLabel,
            String statusCssClass,
            String sourceTypeLabel,
            String sourceCssClass,
            Integer viewCount
    ) {
        this.policyId = policyId;
        this.externalId = externalId;
        this.title = title;
        this.organization = organization;
        this.sourceName = sourceName;
        this.category = category;
        this.categoryLabel = categoryLabel;
        this.target = target;
        this.region = region;
        this.district = district;
        this.startDate = startDate;
        this.endDate = endDate;
        this.applicationPeriodText = applicationPeriodText;
        this.applyStatus = applyStatus;
        this.applyStatusLabel = applyStatusLabel;
        this.status = status;
        this.statusLabel = statusLabel;
        this.statusCssClass = statusCssClass;
        this.sourceTypeLabel = sourceTypeLabel;
        this.sourceCssClass = sourceCssClass;
        this.viewCount = viewCount;
    }

    public static AdminPolicyDTO from(Policy policy) {
        String sourceName = policy.getSource() != null
                ? policy.getSource().getSourceName()
                : "-";

        String sourceTypeLabel = resolveSourceTypeLabel(policy);
        String sourceCssClass = resolveSourceCssClass(policy);

        PolicyCategory category = policy.getCategory();
        ApplyStatus applyStatus = policy.getApplyStatus();
        PolicyStatus status = policy.getStatus();

        return new AdminPolicyDTO(
                policy.getPolicyId(),
                policy.getExternalId(),
                policy.getTitle(),
                sourceName,
                sourceName,
                category,
                category != null ? category.getLabel() : "-",
                policy.getTarget(),
                policy.getRegion(),
                policy.getDistrict(),
                policy.getStartDate(),
                policy.getEndDate(),
                formatApplicationPeriod(
                        policy.getStartDate(),
                        policy.getEndDate(),
                        applyStatus
                ),
                applyStatus,
                applyStatus != null ? applyStatus.getLabel() : "-",
                status,
                getStatusLabel(status),
                getStatusCssClass(status),
                sourceTypeLabel,
                sourceCssClass,
                policy.getViewCount()
        );
    }

    private static String formatApplicationPeriod(
            LocalDate startDate,
            LocalDate endDate,
            ApplyStatus applyStatus
    ) {
        if (applyStatus == ApplyStatus.ALWAYS) {
            return "상시";
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy.MM.dd");

        if (startDate != null && endDate != null) {
            return startDate.format(formatter)
                    + " ~ "
                    + endDate.format(formatter);
        }

        if (startDate != null) {
            return startDate.format(formatter) + "부터";
        }

        if (endDate != null) {
            return endDate.format(formatter) + "까지";
        }

        return "-";
    }

    private static String getStatusLabel(PolicyStatus status) {
        if (status == null) {
            return "-";
        }

        return switch (status) {
            case AUTO_PUBLISHED -> "자동 공개";
            case APPROVED -> "공개";
            case PENDING_REVIEW -> "검수 대기";
            case NEEDS_UPDATE -> "수정 필요";
            case EXPIRED -> "마감";
            case HIDDEN -> "숨김";
            case REJECTED -> "반려";
        };
    }

    private static String getStatusCssClass(PolicyStatus status) {
        if (status == null) {
            return "";
        }

        return switch (status) {
            case AUTO_PUBLISHED, APPROVED -> "ok";
            case PENDING_REVIEW -> "wait";
            case NEEDS_UPDATE -> "ai";
            case EXPIRED -> "expired";
            case HIDDEN -> "hidden";
            case REJECTED -> "bad";
        };
    }

    private static String resolveSourceTypeLabel(Policy policy) {
        if (policy.getSource() == null
                || policy.getSource().getSourceType() == null) {
            return "-";
        }

        return switch (policy.getSource().getSourceType()) {
            case OPEN_API -> "API";
            default -> policy.getSource().getSourceType().name();
        };
    }

    private static String resolveSourceCssClass(Policy policy) {
        if (policy.getSource() == null
                || policy.getSource().getSourceType() == null) {
            return "";
        }

        return switch (policy.getSource().getSourceType()) {
            case OPEN_API -> "api";
            default -> "manual";
        };
    }

}