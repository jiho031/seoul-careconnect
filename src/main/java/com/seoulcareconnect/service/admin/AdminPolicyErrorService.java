package com.seoulcareconnect.service.admin;

import com.seoulcareconnect.dto.admin.AdminPolicyErrorDTO;
import com.seoulcareconnect.entity.policy.PolicyCollectionError;
import com.seoulcareconnect.entity.policy.enums.PolicyErrorStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyErrorType;
import com.seoulcareconnect.repository.policy.PolicyCollectionErrorRepository;
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
public class AdminPolicyErrorService {

    private final PolicyCollectionErrorRepository policyCollectionErrorRepository;

    public long getTotalErrorCount() {
        return policyCollectionErrorRepository.countByStatusNot(
                PolicyErrorStatus.COMPLETED
        );
    }

    public long getMissingValueCount() {
        return policyCollectionErrorRepository.countByErrorTypeAndStatusNot(
                PolicyErrorType.REQUIRED_VALUE_MISSING,
                PolicyErrorStatus.COMPLETED
        );
    }

    public long getDateErrorCount() {
        return policyCollectionErrorRepository.countByErrorTypeAndStatusNot(
                PolicyErrorType.DATE_ERROR,
                PolicyErrorStatus.COMPLETED
        );
    }

    public long getUrlErrorCount() {
        return policyCollectionErrorRepository.countByErrorTypeAndStatusNot(
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
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Specification<PolicyCollectionError> specification =
                Specification.<PolicyCollectionError>unrestricted()
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
        PolicyCollectionError error = policyCollectionErrorRepository
                .findById(errorId)
                .orElseThrow(() ->
                        new IllegalArgumentException("정책 오류 항목을 찾을 수 없습니다.")
                );

        error.setStatus(status);
        error.setAdminMemo(adminMemo);

        if (status == PolicyErrorStatus.COMPLETED
                || status == PolicyErrorStatus.EXCLUDED) {
            error.setProcessedAt(LocalDateTime.now());
        } else {
            error.setProcessedAt(null);
        }
    }

    private Specification<PolicyCollectionError> fetchAssociations() {
        return (root, query, criteriaBuilder) -> {
            if (query != null
                    && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {
                root.fetch("source", JoinType.LEFT);
                root.fetch("policy", JoinType.LEFT);
                query.distinct(true);
            }

            return criteriaBuilder.conjunction();
        };
    }

    private Specification<PolicyCollectionError> keywordContains(
            String keyword
    ) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }

            String searchKeyword =
                    "%" + keyword.trim().toLowerCase() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("policyTitle")),
                            searchKeyword
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("externalId")),
                            searchKeyword
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.join("source", JoinType.LEFT)
                                            .get("sourceName")
                            ),
                            searchKeyword
                    )
            );
        };
    }

    private Specification<PolicyCollectionError> errorTypeEquals(
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

    private Specification<PolicyCollectionError> statusEquals(
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

    private Specification<PolicyCollectionError> sourceIdEquals(
            Long sourceId
    ) {
        return (root, query, criteriaBuilder) -> {
            if (sourceId == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    root.join("source", JoinType.LEFT)
                            .get("sourceId"),
                    sourceId
            );
        };
    }
}