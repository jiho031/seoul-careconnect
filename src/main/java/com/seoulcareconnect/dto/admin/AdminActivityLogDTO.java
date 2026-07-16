package com.seoulcareconnect.dto.admin;

import com.seoulcareconnect.entity.admin.AdminActivityLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class AdminActivityLogDTO {

    private Long activityLogId;

    private Long adminId;

    private String adminName;

    private String activityType;

    private String targetName;

    private String description;

    private LocalDateTime createdAt;

    public static AdminActivityLogDTO from(
            AdminActivityLog log
    ) {
        return AdminActivityLogDTO.builder()
                .activityLogId(
                        log.getActivityLogId()
                )
                .adminId(
                        log.getAdminId()
                )
                .adminName(
                        safeText(
                                log.getAdminName(),
                                "관리자"
                        )
                )
                .activityType(
                        log.getActivityType() == null
                                ? "관리자 활동"
                                : log.getActivityType().getLabel()
                )
                .targetName(
                        safeText(
                                log.getTargetName(),
                                "-"
                        )
                )
                .description(
                        safeText(
                                log.getDescription(),
                                "-"
                        )
                )
                .createdAt(
                        log.getCreatedAt()
                )
                .build();
    }

    public String getCreatedAtText() {
        if (createdAt == null) {
            return "-";
        }

        return createdAt.format(
                DateTimeFormatter.ofPattern(
                        "yyyy.MM.dd HH:mm"
                )
        );
    }

    private static String safeText(
            String value,
            String fallback
    ) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value;
    }

    public String getDashboardTimeText() {
        if (createdAt == null) {
            return "-";
        }

        LocalDate today = LocalDate.now();

        if (createdAt.toLocalDate().isEqual(today)) {
            return createdAt.format(
                    DateTimeFormatter.ofPattern("HH:mm")
            );
        }

        return createdAt.format(
                DateTimeFormatter.ofPattern("MM.dd HH:mm")
        );
    }
}