package com.seoulcareconnect.dto.admin;

import com.seoulcareconnect.entity.policy.enums.SyncStatus;
import com.seoulcareconnect.entity.policy.enums.SyncType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class AdminSyncLogDTO {

    private Long logId;
    private int duplicateCount;

    // 수집처 정보
    private Long sourceId;
    private String sourceName;

    // 수집 방식: 자동, 수동, 재시도
    private SyncType syncType;

    // 수집 결과 상태
    private SyncStatus status;

    // 수집 처리 건수
    private int successCount;
    private int failCount;

    // 오류 내용
    private String errorMessage;

    // 실행 시간
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    public int getTotalCount() {
        return successCount + failCount;
    }

    public String getSyncTypeLabel() {
        if (syncType == null) {
            return "-";
        }

        return switch (syncType) {
            case SCHEDULED -> "자동 수집";
            case MANUAL -> "수동 수집";
            case RETRY -> "재수집";
        };
    }

    public String getStatusLabel() {
        if (status == null) {
            return "확인 불가";
        }

        return switch (status) {
            case SUCCESS -> "성공";
            case PARTIAL -> "일부 실패";
            case FAIL -> "실패";
        };
    }

    public String getStatusCssClass() {
        if (status == null) {
            return "wait";
        }

        return switch (status) {
            case SUCCESS -> "success";
            case PARTIAL -> "warning";
            case FAIL -> "danger";
        };
    }

    public String getStartedAtText() {
        return formatDateTime(startedAt);
    }

    public String getEndedAtText() {
        return formatDateTime(endedAt);
    }

    public String getDurationText() {
        if (startedAt == null || endedAt == null) {
            return "-";
        }

        long seconds = java.time.Duration
                .between(startedAt, endedAt)
                .getSeconds();

        if (seconds < 60) {
            return seconds + "초";
        }

        long minutes = seconds / 60;
        long remainSeconds = seconds % 60;

        return minutes + "분 " + remainSeconds + "초";
    }

    public String getSafeErrorMessage() {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "-";
        }

        return errorMessage;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }

        return dateTime.format(
                DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")
        );
    }

    public String getProcessingSummary() {
        return String.format(
                "저장 %d건 · 실패 %d건 · 중복 %d건",
                successCount,
                failCount,
                duplicateCount
        );
    }
}