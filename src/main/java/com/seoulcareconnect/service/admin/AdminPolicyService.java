package com.seoulcareconnect.service.admin;

import com.seoulcareconnect.dto.admin.AdminPolicyDTO;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.entity.policy.enums.PolicyStatus;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPolicyService {

    private final PolicyRepository policyRepository;

    /*
     * 상단 통계
     */

    public long getTotalPolicyCount() {
        return policyRepository.count();
    }

    public long getPublishedCount() {
        return policyRepository.countByStatusIn(
                List.of(
                        PolicyStatus.AUTO_PUBLISHED,
                        PolicyStatus.APPROVED
                )
        );
    }

    public long getPendingReviewCount() {
        return policyRepository.countByStatusIn(
                List.of(
                        PolicyStatus.PENDING_REVIEW,
                        PolicyStatus.NEEDS_UPDATE
                )
        );
    }

    public long getHiddenOrExpiredCount() {
        return policyRepository.countByStatusIn(
                List.of(
                        PolicyStatus.HIDDEN,
                        PolicyStatus.EXPIRED
                )
        );
    }

    /*
     * 정책 검색·필터·페이지네이션
     */

    public Page<AdminPolicyDTO> getPolicies(
            String keyword,
            PolicyCategory category,
            PolicyStatus status,
            Long sourceId,
            String district,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("policyId")
                )
        );

        Specification<Policy> specification =
                Specification.<Policy>unrestricted()
                        .and(fetchAssociations())
                        .and(keywordContains(keyword))
                        .and(categoryEquals(category))
                        .and(statusEquals(status))
                        .and(sourceIdEquals(sourceId))
                        .and(districtEquals(district));

        return policyRepository
                .findAll(specification, pageable)
                .map(AdminPolicyDTO::from);
    }

    public List<AdminPolicyDTO> getDownloadPolicies(
            String keyword,
            PolicyCategory category,
            PolicyStatus status,
            Long sourceId,
            String district
    ) {
        Specification<Policy> specification =
                Specification.<Policy>unrestricted()
                        .and(fetchAssociations())
                        .and(keywordContains(keyword))
                        .and(categoryEquals(category))
                        .and(statusEquals(status))
                        .and(sourceIdEquals(sourceId))
                        .and(districtEquals(district));

        return policyRepository
                .findAll(
                        specification,
                        Sort.by(
                                Sort.Order.desc("createdAt"),
                                Sort.Order.desc("policyId")
                        )
                )
                .stream()
                .map(AdminPolicyDTO::from)
                .toList();
    }

    @Transactional
    public void approvePolicy(Long policyId) {
        Policy policy = findPolicy(policyId);

        if (policy.getStatus() == PolicyStatus.EXPIRED) {
            throw new IllegalStateException(
                    "마감된 정책은 공개 승인할 수 없습니다."
            );
        }

        policy.setStatus(PolicyStatus.APPROVED);
    }

    @Transactional
    public void hidePolicy(Long policyId) {
        Policy policy = findPolicy(policyId);
        policy.setStatus(PolicyStatus.HIDDEN);
    }


    @Transactional
    public void unhidePolicy(Long policyId) {
        Policy policy = findPolicy(policyId);

        if (policy.getStatus() != PolicyStatus.HIDDEN) {
            throw new IllegalStateException(
                    "숨김 상태인 정책만 숨김 해제할 수 있습니다."
            );
        }

        policy.setStatus(PolicyStatus.PENDING_REVIEW);
    }


    @Transactional
    public void markNeedsUpdate(Long policyId) {
        Policy policy = findPolicy(policyId);

        if (policy.getStatus() == PolicyStatus.EXPIRED
                || policy.getStatus() == PolicyStatus.HIDDEN) {
            throw new IllegalStateException(
                    "마감 또는 숨김 정책은 수정 필요 상태로 변경할 수 없습니다."
            );
        }

        policy.setStatus(PolicyStatus.NEEDS_UPDATE);
    }


    @Transactional
    public void rejectPolicy(Long policyId) {
        Policy policy = findPolicy(policyId);
        policy.setStatus(PolicyStatus.REJECTED);
    }


    private Policy findPolicy(Long policyId) {
        return policyRepository.findById(policyId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "정책을 찾을 수 없습니다."
                        )
                );
    }


    private Specification<Policy> fetchAssociations() {
        return (root, query, criteriaBuilder) -> {
            if (query != null
                    && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {

                root.fetch("source", JoinType.LEFT);
                root.fetch("detail", JoinType.LEFT);

                query.distinct(true);
            }

            return criteriaBuilder.conjunction();
        };
    }


    private Specification<Policy> keywordContains(
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
                            criteriaBuilder.lower(
                                    root.get("title")
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
                                    root.get("target")
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


    private Specification<Policy> categoryEquals(
            PolicyCategory category
    ) {
        return (root, query, criteriaBuilder) -> {
            if (category == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    root.get("category"),
                    category
            );
        };
    }


    private Specification<Policy> statusEquals(
            PolicyStatus status
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


    private Specification<Policy> sourceIdEquals(
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


    private Specification<Policy> districtEquals(
            String district
    ) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(district)) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    root.get("district"),
                    district.trim()
            );
        };
    }

    public List<AdminPolicyDTO> getRecentReviewPolicies() {

        Pageable pageable = PageRequest.of(
                0,
                20
        );

        return policyRepository
                .findByStatusInOrderByUpdatedAtDescCreatedAtDesc(
                        List.of(
                                PolicyStatus.PENDING_REVIEW,
                                PolicyStatus.NEEDS_UPDATE
                        ),
                        pageable
                )
                .stream()
                .map(AdminPolicyDTO::from)
                .toList();
    }
}