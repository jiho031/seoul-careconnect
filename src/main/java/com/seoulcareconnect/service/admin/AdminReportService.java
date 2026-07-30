package com.seoulcareconnect.service.admin;

import com.seoulcareconnect.dto.admin.AdminReportDTO;
import com.seoulcareconnect.entity.policy.enums.PolicyErrorStatus;
import com.seoulcareconnect.entity.report.MissingPolicyReport;
import com.seoulcareconnect.repository.policy.PolicyCollectionErrorRepository;
import com.seoulcareconnect.repository.report.MissingPolicyReportRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {

    private final MissingPolicyReportRepository
            missingPolicyReportRepository;

    private final PolicyCollectionErrorRepository
            policyCollectionErrorRepository;

    public long getTotalCount() {
        return missingPolicyReportRepository.count();
    }

    public long getReceivedCount() {
        return missingPolicyReportRepository
                .countByStatus("RECEIVED");
    }

    public long getInReviewCount() {
        return missingPolicyReportRepository
                .countByStatus("IN_REVIEW");
    }

    public long getCompletedCount() {
        return missingPolicyReportRepository
                .countByStatus("COMPLETED");
    }

    public Page<AdminReportDTO> getReports(
            String keyword,
            String reportType,
            String status,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        Specification<MissingPolicyReport> specification =
                Specification
                        .<MissingPolicyReport>unrestricted()
                        .and(fetchAssociations())
                        .and(keywordContains(keyword))
                        .and(reportTypeEquals(reportType))
                        .and(statusEquals(status));

        return missingPolicyReportRepository
                .findAll(specification, pageable)
                .map(AdminReportDTO::from);
    }

    /**
     * 사용자 신고 상태 변경
     *
     * 연결된 정책 오류가 있으면 정책 오류 상태도 함께 변경한다.
     */
    @Transactional
    public void changeStatus(
            Long reportId,
            String status,
            String adminMemo
    ) {
        MissingPolicyReport report =
                missingPolicyReportRepository
                        .findById(reportId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "사용자 신고를 찾을 수 없습니다."
                                )
                        );

        validateStatus(status);

        report.setStatus(status);
        report.setAdminMemo(
                trimToNull(adminMemo)
        );

        LocalDateTime processedAt =
                resolveProcessedAt(status);

        report.setProcessedAt(processedAt);

        synchronizePolicyErrorStatus(
                reportId,
                status,
                adminMemo,
                processedAt
        );
    }

    /**
     * 사용자 신고 상태를 연결된 정책 오류 상태에 반영한다.
     *
     * RECEIVED  -> WAITING
     * IN_REVIEW -> IN_PROGRESS
     * COMPLETED -> COMPLETED
     * REJECTED  -> EXCLUDED
     */
    private void synchronizePolicyErrorStatus(
            Long reportId,
            String reportStatus,
            String adminMemo,
            LocalDateTime processedAt
    ) {
        policyCollectionErrorRepository
                .findByReport_ReportId(reportId)
                .ifPresent(error -> {

                    PolicyErrorStatus errorStatus =
                            convertToPolicyErrorStatus(
                                    reportStatus
                            );

                    error.setStatus(errorStatus);

                    error.setAdminMemo(
                            trimToNull(adminMemo)
                    );

                    if (errorStatus
                            == PolicyErrorStatus.COMPLETED
                            || errorStatus
                            == PolicyErrorStatus.EXCLUDED) {

                        error.setProcessedAt(
                                processedAt != null
                                        ? processedAt
                                        : LocalDateTime.now()
                        );

                    } else {
                        error.setProcessedAt(null);
                    }
                });
    }

    private PolicyErrorStatus convertToPolicyErrorStatus(
            String reportStatus
    ) {
        return switch (reportStatus) {
            case "IN_REVIEW" ->
                    PolicyErrorStatus.IN_PROGRESS;

            case "COMPLETED" ->
                    PolicyErrorStatus.COMPLETED;

            case "REJECTED" ->
                    PolicyErrorStatus.EXCLUDED;

            case "RECEIVED" ->
                    PolicyErrorStatus.WAITING;

            default ->
                    throw new IllegalArgumentException(
                            "정책 오류 상태로 변환할 수 없는 신고 상태입니다."
                    );
        };
    }

    private LocalDateTime resolveProcessedAt(
            String status
    ) {
        if ("COMPLETED".equals(status)
                || "REJECTED".equals(status)) {

            return LocalDateTime.now();
        }

        return null;
    }

    private Specification<MissingPolicyReport>
    fetchAssociations() {

        return (root, query, criteriaBuilder) -> {
            if (query != null
                    && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {

                root.fetch(
                        "user",
                        JoinType.LEFT
                );

                root.fetch(
                        "policy",
                        JoinType.LEFT
                );

                query.distinct(true);
            }

            return criteriaBuilder.conjunction();
        };
    }

    private Specification<MissingPolicyReport>
    keywordContains(
            String keyword
    ) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }

            String searchKeyword =
                    "%"
                            + keyword.trim().toLowerCase()
                            + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.get("title")
                            ),
                            searchKeyword
                    ),

                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.get("content")
                            ),
                            searchKeyword
                    ),

                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.get("sourceUrl")
                            ),
                            searchKeyword
                    ),

                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.join(
                                                    "user",
                                                    JoinType.LEFT
                                            )
                                            .get("name")
                            ),
                            searchKeyword
                    ),

                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.join(
                                                    "user",
                                                    JoinType.LEFT
                                            )
                                            .get("email")
                            ),
                            searchKeyword
                    ),

                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.join(
                                                    "policy",
                                                    JoinType.LEFT
                                            )
                                            .get("title")
                            ),
                            searchKeyword
                    )
            );
        };
    }

    private Specification<MissingPolicyReport>
    reportTypeEquals(
            String reportType
    ) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(reportType)) {
                return criteriaBuilder.conjunction();
            }

            if ("MISSING".equals(reportType)
                    || "MISSING_POLICY".equals(reportType)) {
                return root.get("reportType").in(
                        "MISSING",
                        "MISSING_POLICY"
                );
            }

            return criteriaBuilder.equal(
                    root.get("reportType"),
                    reportType
            );
        };
    }

    private Specification<MissingPolicyReport>
    statusEquals(
            String status
    ) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(status)) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    root.get("status"),
                    status
            );
        };
    }

    private void validateStatus(
            String status
    ) {
        if (!"RECEIVED".equals(status)
                && !"IN_REVIEW".equals(status)
                && !"COMPLETED".equals(status)
                && !"REJECTED".equals(status)) {

            throw new IllegalArgumentException(
                    "올바르지 않은 신고 처리 상태입니다."
            );
        }
    }

    private String trimToNull(
            String value
    ) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}