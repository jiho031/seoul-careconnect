package com.seoulcareconnect.service.admin;

import com.seoulcareconnect.dto.admin.AdminActivityLogDTO;
import com.seoulcareconnect.entity.admin.AdminActivityLog;
import com.seoulcareconnect.entity.admin.enums.AdminActivityType;
import com.seoulcareconnect.repository.admin.AdminActivityLogRepository;
import lombok.RequiredArgsConstructor;
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

        String trimmed = value.trim();

        if (trimmed.length() <= max) {
            return trimmed;
        }

        return trimmed.substring(0, max);
    }

    @Transactional(readOnly = true)
    public List<AdminActivityLogDTO> getRecentFourteenDayActivities() {

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
}