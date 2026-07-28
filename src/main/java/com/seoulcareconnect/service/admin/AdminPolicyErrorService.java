package com.seoulcareconnect.service.admin;

import com.seoulcareconnect.dto.admin.AdminPolicyErrorDTO;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.PolicyCollectionError;
import com.seoulcareconnect.entity.policy.PolicyDetail;
import com.seoulcareconnect.entity.policy.enums.PolicyErrorStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyErrorType;
import com.seoulcareconnect.entity.policy.enums.PolicyStatus;
import com.seoulcareconnect.entity.report.MissingPolicyReport;
import com.seoulcareconnect.repository.policy.PolicyCollectionErrorRepository;
import com.seoulcareconnect.repository.policy.PolicyRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPolicyErrorService {

    private final PolicyCollectionErrorRepository
            policyCollectionErrorRepository;

    private final PolicyRepository policyRepository;

    public long getTotalErrorCount() {
        return policyCollectionErrorRepository.countByStatusNot(
                PolicyErrorStatus.COMPLETED
        );
    }

    public long getMissingValueCount() {
        return policyCollectionErrorRepository
                .countByErrorTypeAndStatusNot(
                        PolicyErrorType.REQUIRED_VALUE_MISSING,
                        PolicyErrorStatus.COMPLETED
                );
    }

    public long getDateErrorCount() {
        return policyCollectionErrorRepository
                .countByErrorTypeAndStatusNot(
                        PolicyErrorType.DATE_ERROR,
                        PolicyErrorStatus.COMPLETED
                );
    }

    public long getUrlErrorCount() {
        return policyCollectionErrorRepository
                .countByErrorTypeAndStatusNot(
                        PolicyErrorType.URL_ERROR,
                        PolicyErrorStatus.COMPLETED
                );
    }

    public Page<AdminPolicyErrorDTO> getPolicyErrors(
            String keyword,
            PolicyErrorType errorType,
            PolicyErrorStatus status,
            Long sourceId,
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

        Specification<PolicyCollectionError> specification =
                Specification
                        .<PolicyCollectionError>unrestricted()
                        .and(fetchAssociations())
                        .and(keywordContains(keyword))
                        .and(errorTypeEquals(errorType))
                        .and(statusEquals(status))
                        .and(sourceIdEquals(sourceId));

        return policyCollectionErrorRepository
                .findAll(specification, pageable)
                .map(AdminPolicyErrorDTO::from);
    }

    @Transactional
    public void changeStatus(
            Long errorId,
            PolicyErrorStatus status,
            String adminMemo
    ) {
        PolicyCollectionError error =
                policyCollectionErrorRepository
                        .findById(errorId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "정책 오류 항목을 찾을 수 없습니다."
                                )
                        );

        error.setStatus(status);
        error.setAdminMemo(adminMemo);

        LocalDateTime processedAt = null;

        if (status == PolicyErrorStatus.COMPLETED
                || status == PolicyErrorStatus.EXCLUDED) {

            processedAt = LocalDateTime.now();
        }

        error.setProcessedAt(processedAt);

        MissingPolicyReport report =
                error.getReport();

        if (report != null) {
            String reportStatus =
                    switch (status) {
                        case WAITING ->
                                "RECEIVED";

                        case IN_PROGRESS ->
                                "IN_REVIEW";

                        case COMPLETED ->
                                "COMPLETED";

                        case EXCLUDED ->
                                "REJECTED";
                    };

            report.setStatus(reportStatus);
            report.setAdminMemo(adminMemo);
            report.setProcessedAt(processedAt);
        }
    }

    /**
     * 정책 오류 모달에서 정책 직접 수정
     */
    @Transactional
    public void quickEdit(
            Long errorId,
            String title,
            String target,
            String region,
            String district,
            LocalDate startDate,
            LocalDate endDate,
            String officialUrl,
            String benefit,
            String selectionCriteria,
            String requiredDocumentsText,
            String contentText,
            String adminMemo
    ) {
        PolicyCollectionError error =
                findError(errorId);

        Policy policy = error.getPolicy();

        if (policy == null) {
            throw new IllegalStateException(
                    "연결된 정책이 없어 모달에서 직접 수정할 수 없습니다."
            );
        }

        validateDates(
                startDate,
                endDate
        );

        if (StringUtils.hasText(title)) {
            policy.setTitle(title.trim());
        }

        policy.setTarget(
                trimToNull(target)
        );

        policy.setRegion(
                trimToNull(region)
        );

        policy.setDistrict(
                trimToNull(district)
        );

        policy.setStartDate(startDate);
        policy.setEndDate(endDate);

        policy.setOfficialUrl(
                trimToNull(officialUrl)
        );

        /*
         * 수정 후 최종 승인 전 검수 대기로 변경한다.
         * 최근 검수 필요 정책 목록에도 자동 표시된다.
         */
        policy.setStatus(
                PolicyStatus.PENDING_REVIEW
        );

        PolicyDetail detail =
                policy.getDetail();

        boolean hasDetailValue =
                StringUtils.hasText(benefit)
                        || StringUtils.hasText(selectionCriteria)
                        || StringUtils.hasText(requiredDocumentsText)
                        || StringUtils.hasText(contentText);

        if (detail == null && hasDetailValue) {
            detail = new PolicyDetail();
            policy.attachDetail(detail);
        }

        if (detail != null) {
            detail.setBenefit(
                    trimToNull(benefit)
            );

            detail.setSelectionCriteria(
                    trimToNull(selectionCriteria)
            );

            detail.setRequiredDocumentsText(
                    trimToNull(requiredDocumentsText)
            );

            detail.setContentText(
                    trimToNull(contentText)
            );
        }

        policyRepository.save(policy);

        /*
         * 직접 수정이 완료된 오류는 처리 완료로 변경한다.
         */
        error.setStatus(
                PolicyErrorStatus.COMPLETED
        );

        error.setAdminMemo(
                trimToNull(adminMemo)
        );

        error.setProcessedAt(
                LocalDateTime.now()
        );
    }

    private PolicyCollectionError findError(
            Long errorId
    ) {
        return policyCollectionErrorRepository
                .findById(errorId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "정책 오류 항목을 찾을 수 없습니다."
                        )
                );
    }

    private void validateDates(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate != null
                && endDate != null
                && startDate.isAfter(endDate)) {

            throw new IllegalArgumentException(
                    "신청 시작일은 종료일보다 늦을 수 없습니다."
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

    private Specification<PolicyCollectionError>
    fetchAssociations() {

        return (root, query, criteriaBuilder) -> {
            if (query != null
                    && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {

                root.fetch(
                        "source",
                        JoinType.LEFT
                );

                var policyFetch =
                        root.fetch(
                                "policy",
                                JoinType.LEFT
                        );

                policyFetch.fetch(
                        "detail",
                        JoinType.LEFT
                );

                query.distinct(true);
            }

            return criteriaBuilder.conjunction();
        };
    }

    private Specification<PolicyCollectionError>
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
                                    root.get("policyTitle")
                            ),
                            searchKeyword
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.get("externalId")
                            ),
                            searchKeyword
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.join(
                                                    "source",
                                                    JoinType.LEFT
                                            )
                                            .get("sourceName")
                            ),
                            searchKeyword
                    )
            );
        };
    }

    private Specification<PolicyCollectionError>
    errorTypeEquals(
            PolicyErrorType errorType
    ) {
        return (root, query, criteriaBuilder) -> {
            if (errorType == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    root.get("errorType"),
                    errorType
            );
        };
    }

    private Specification<PolicyCollectionError>
    statusEquals(
            PolicyErrorStatus status
    ) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    root.get("status"),
                    status
            );
        };
    }

    private Specification<PolicyCollectionError>
    sourceIdEquals(
            Long sourceId
    ) {
        return (root, query, criteriaBuilder) -> {
            if (sourceId == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    root.join(
                                    "source",
                                    JoinType.LEFT
                            )
                            .get("sourceId"),
                    sourceId
            );
        };
    }
}