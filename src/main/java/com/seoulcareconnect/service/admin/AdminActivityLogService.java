package com.seoulcareconnect.service.admin;

import com.seoulcareconnect.dto.admin.AdminActivityLogDTO;
import com.seoulcareconnect.entity.admin.AdminActivityLog;
import com.seoulcareconnect.entity.admin.enums.AdminActivityType;
import com.seoulcareconnect.repository.admin.AdminActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminActivityLogService {

    private final AdminActivityLogRepository
            activityLogRepository;

    /**
     * 관리자 활동 저장
     */
    @Transactional
    public void record(
            Long adminId,
            String adminName,
            AdminActivityType activityType,
            Long targetId,
            String targetName,
            String description
    ) {
        AdminActivityLog log =
                new AdminActivityLog();

        log.setAdminId(adminId);

        log.setAdminName(
                safeText(
                        adminName,
                        "관리자"
                )
        );

        log.setActivityType(
                activityType == null
                        ? AdminActivityType.OTHER
                        : activityType
        );

        log.setTargetId(targetId);

        log.setTargetName(
                limit(
                        targetName,
                        300
                )
        );

        log.setDescription(
                limit(
                        safeText(
                                description,
                                "관리자 활동이 수행되었습니다."
                        ),
                        1000
                )
        );

        activityLogRepository.save(log);
    }

    /**
     * 현재 로그인한 관리자를 기준으로 활동 저장
     *
     * 현재 인증 구조에서 관리자 ID를 바로 알 수 없으므로
     * 관리자 이름은 Authentication의 name을 사용하고,
     * 관리자 ID는 null로 저장한다.
     */
    @Transactional
    public void recordCurrentAdmin(
            AdminActivityType activityType,
            Long targetId,
            String targetName,
            String description
    ) {
        record(
                null,
                getCurrentAdminName(),
                activityType,
                targetId,
                targetName,
                description
        );
    }

    /**
     * 대시보드 최근 관리자 활동 5건
     */
    @Transactional(readOnly = true)
    public List<AdminActivityLogDTO>
    getRecentActivities() {

        return activityLogRepository
                .findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(AdminActivityLogDTO::from)
                .toList();
    }

    /**
     * 최근 14일 관리자 활동
     */
    @Transactional(readOnly = true)
    public List<AdminActivityLogDTO>
    getRecentFourteenDayActivities() {

        LocalDateTime cutoffDateTime =
                LocalDateTime.now().minusDays(14);

        return activityLogRepository
                .findAllByCreatedAtAfterOrderByCreatedAtDesc(
                        cutoffDateTime
                )
                .stream()
                .map(AdminActivityLogDTO::from)
                .toList();
    }

    private String getCurrentAdminName() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()
                || "anonymousUser".equals(
                authentication.getName()
        )) {

            return "관리자";
        }

        return authentication
                .getName()
                .trim();
    }

    private String safeText(
            String value,
            String fallback
    ) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }

    private String limit(
            String value,
            int max
    ) {
        if (value == null) {
            return null;
        }

        String trimmed =
                value.trim();

        if (trimmed.length() <= max) {
            return trimmed;
        }

        return trimmed.substring(
                0,
                max
        );
    }
}