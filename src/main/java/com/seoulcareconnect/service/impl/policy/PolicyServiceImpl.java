package com.seoulcareconnect.service.impl.policy;

import com.seoulcareconnect.dto.policy.PolicyDTO;
import com.seoulcareconnect.dto.policy.PolicySearchDTO;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyStatus;
import com.seoulcareconnect.mapper.policy.PolicyMapper;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import com.seoulcareconnect.repository.policy.PolicyViewLogRepository;
import com.seoulcareconnect.service.policy.PolicyService;
import com.seoulcareconnect.specification.policy.PolicySpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyServiceImpl implements PolicyService {

    private static final List<PolicyStatus> PUBLIC_STATUSES = List.of(
            PolicyStatus.AUTO_PUBLISHED,
            PolicyStatus.APPROVED
    );

    private final PolicyRepository policyRepository;
    private final PolicyViewLogRepository policyViewLogRepository;
    private final PolicyMapper policyMapper;

    @Value("${app.policy.sync.zone-id:Asia/Seoul}")
    private String zoneId;

    @Value("${app.policy.query.default-page-size:4}")
    private int defaultPageSize;

    @Value("${app.policy.query.max-page-size:20}")
    private int maxPageSize;

    @Value("${app.policy.query.new-days:14}")
    private int newDays;

    @Override
    public Page<PolicyDTO> search(PolicySearchDTO search) {
        if ("recent".equals(search.safeQuick())) {
            return Page.empty(PageRequest.of(
                    search.safePage(),
                    search.safeSize(defaultPageSize, maxPageSize)
            ));
        }

        Sort sort = switch (search.safeSort()) {
            case "latest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "popular" -> Sort.by(
                    Sort.Order.desc("viewCount"),
                    Sort.Order.desc("createdAt")
            );
            default -> Sort.unsorted();
        };

        Pageable pageable = PageRequest.of(
                search.safePage(),
                search.safeSize(defaultPageSize, maxPageSize),
                sort
        );

        Page<Policy> page = policyRepository.findAll(
                PolicySpecification.build(search, today(), newDays),
                pageable
        );
        return page.map(policyMapper::toDto);
    }

    @Override
    public List<PolicyDTO> closingSoon(int limit) {
        return policyRepository.findClosingSoon(
                        PUBLIC_STATUSES,
                        ApplyStatus.EXPIRED,
                        today(),
                        PageRequest.of(0, safeLimit(limit, 4))
                ).stream()
                .map(policyMapper::toDto)
                .toList();
    }

    @Override
    public List<PolicyDTO> recent(int limit) {
        return policyRepository.findRecent(
                        PUBLIC_STATUSES,
                        ApplyStatus.EXPIRED,
                        today(),
                        PageRequest.of(0, safeLimit(limit, 4))
                ).stream()
                .map(policyMapper::toDto)
                .toList();
    }

    @Override
    public List<PolicyDTO> popular(int limit) {
        return policyRepository.findPopular(
                        PUBLIC_STATUSES,
                        ApplyStatus.EXPIRED,
                        today(),
                        PageRequest.of(0, safeLimit(limit, 5))
                ).stream()
                .map(policyMapper::toDto)
                .toList();
    }

    @Override
    public Page<PolicyDTO> recentViewed(Long userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), maxPageSize);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        if (userId == null) return Page.empty(pageable);

        List<Long> ids = policyViewLogRepository.findRecentPolicyIdsByUserId(
                userId,
                PageRequest.of(0, 200)
        );

        List<Long> distinctIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (distinctIds.isEmpty()) return Page.empty(pageable);

        List<Policy> policies = policyRepository.findAllByPolicyIdInAndStatusInAndApplyStatusNot(
                distinctIds,
                PUBLIC_STATUSES,
                ApplyStatus.EXPIRED
        );

        Map<Long, Policy> byId = policies.stream().collect(Collectors.toMap(
                Policy::getPolicyId,
                Function.identity(),
                (left, right) -> left
        ));

        List<PolicyDTO> ordered = distinctIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .filter(this::isCurrentlyVisible)
                .map(policyMapper::toDto)
                .toList();

        int from = Math.min(safePage * safeSize, ordered.size());
        int to = Math.min(from + safeSize, ordered.size());
        return new PageImpl<>(ordered.subList(from, to), pageable, ordered.size());
    }

    private boolean isCurrentlyVisible(Policy policy) {
        LocalDate today = today();
        return (policy.getStartDate() == null || !policy.getStartDate().isAfter(today))
                && (policy.getEndDate() == null || !policy.getEndDate().isBefore(today));
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of(zoneId));
    }

    private int safeLimit(int limit, int defaultLimit) {
        if (limit < 1) return defaultLimit;
        return Math.min(limit, 50);
    }
}
