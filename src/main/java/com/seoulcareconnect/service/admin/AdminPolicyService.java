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

    /*
     * 정책 승인
     */

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

    /*
     * 정책 숨김
     */

    @Transactional
    public void hidePolicy(Long policyId) {
        Policy policy = findPolicy(policyId);
        policy.setStatus(PolicyStatus.HIDDEN);
    }

    /*
     * 숨김 해제
     */

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

    /*
     * 수정 필요 상태
     */

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

    /*
     * 반려
     */

    @Transactional
    public void rejectPolicy(Long policyId) {
        Policy policy = findPolicy(policyId);
        policy.setStatus(PolicyStatus.REJECTED);
    }

    /*
     * 정책 조회
     */

    private Policy findPolicy(Long policyId) {
        return policyRepository.findById(policyId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "정책을 찾을 수 없습니다."
                        )
                );
    }

    /*
     * 연관 엔티티 fetch
     */

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

    /*
     * 정책명·기관명·대상·외부 정책 ID 검색
     */

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

    /*
     * 분야 필터
     */

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

    /*
     * 정책 상태 필터
     */

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

    /*
     * API 출처 필터
     */

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

    /*
     * 지역 필터
     */

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
}