package com.seoulcareconnect.service.admin;

import com.seoulcareconnect.dto.admin.AdminUserDTO;
import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;

    /**
     * 회원 목록 조회
     */
    public Page<AdminUserDTO> getUsers(
            String keyword,
            String role,
            Boolean active,
            String ageGroup,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("userId")
                )
        );

        Specification<User> specification =
                buildSpecification(
                        keyword,
                        role,
                        active,
                        ageGroup
                );

        return userRepository
                .findAll(specification, pageable)
                .map(AdminUserDTO::from);
    }

    /**
     * 전체 회원 수
     */
    public long getTotalUserCount() {
        return userRepository.count();
    }

    /**
     * 오늘 가입 회원 수
     */
    public long getTodayJoinedCount() {
        LocalDate today = LocalDate.now();

        LocalDateTime startOfDay =
                today.atStartOfDay();

        LocalDateTime endOfDay =
                today.plusDays(1).atStartOfDay();

        return userRepository.countByCreatedAtBetween(
                startOfDay,
                endOfDay
        );
    }

    /**
     * 관리자 수
     *
     * 최고 관리자는 별도 집계하고
     * 일반 관리자만 계산한다.
     */
    public long getAdminCount() {
        return userRepository.countByRole("ADMIN");
    }

    /**
     * 비활성 회원 수
     */
    public long getInactiveUserCount() {
        return userRepository.countByIsActiveFalse();
    }

    /**
     * 회원 비활성화
     */
    @Transactional
    public void deactivateUser(Long userId) {
        User user = findUser(userId);

        if ("SUPER_ADMIN".equals(user.getRole())) {
            throw new IllegalStateException(
                    "최고 관리자는 비활성화할 수 없습니다."
            );
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalStateException(
                    "이미 비활성 상태인 회원입니다."
            );
        }

        user.setIsActive(false);
    }

    /**
     * 회원 활성화
     */
    @Transactional
    public void activateUser(Long userId) {
        User user = findUser(userId);

        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalStateException(
                    "이미 활성 상태인 회원입니다."
            );
        }

        user.setIsActive(true);
        user.setDeletedAt(null);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "회원을 찾을 수 없습니다."
                        )
                );
    }

    /**
     * 검색 및 필터 조건 조합
     */
    private Specification<User> buildSpecification(
            String keyword,
            String role,
            Boolean active,
            String ageGroup
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates =
                    new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String searchKeyword =
                        "%"
                                + keyword.trim().toLowerCase()
                                + "%";

                predicates.add(
                        criteriaBuilder.or(
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get("name")
                                        ),
                                        searchKeyword
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get("email")
                                        ),
                                        searchKeyword
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get("phone")
                                        ),
                                        searchKeyword
                                )
                        )
                );
            }

            if (StringUtils.hasText(role)) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("role"),
                                role.trim()
                        )
                );
            }

            if (active != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("isActive"),
                                active
                        )
                );
            }

            if (StringUtils.hasText(ageGroup)) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("ageGroup"),
                                ageGroup.trim()
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(
                            new Predicate[0]
                    )
            );
        };
    }
}