
package com.seoulcareconnect.dto.admin;

import com.seoulcareconnect.entity.policy.enums.SourceType;
import com.seoulcareconnect.entity.policy.enums.SyncStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminSourceStatusDTO {

    private Long sourceId;

    // 수집처 이름
    private String sourceName;

    // OPEN_API, WEB, MANUAL
    private SourceType sourceType;

    // 기본 API 주소
    private String baseUrl;

    // 수집처 활성화 여부
    private boolean active;

    // 수집처가 마지막으로 확인된 시간
    private LocalDateTime lastCheckedAt;

    // 해당 수집처의 가장 최근 수집 상태
    private SyncStatus latestSyncStatus;

    // 최근 수집 시작 시간
    private LocalDateTime latestStartedAt;

    // 최근 수집 종료 시간
    private LocalDateTime latestEndedAt;

    // 최근 수집 성공 건수
    private int latestSuccessCount;

    // 최근 수집 실패 건수
    private int latestFailCount;

    // 최근 오류 메시지
    private String latestErrorMessage;

    public String getSourceTypeLabel() {
        if (sourceType == null) {
            return "-";
        }

        return switch (sourceType) {
            case OPEN_API -> "공공 API";
            case WEB -> "웹 수집";
            case MANUAL -> "수동 등록";
        };
    }

    public String getStatusLabel() {
        if (!active) {
            return "비활성";
        }

        if (latestSyncStatus == null) {
            return "수집 이력 없음";
        }

        return switch (latestSyncStatus) {
            case SUCCESS -> "정상";
            case PARTIAL -> "일부 실패";
            case FAIL -> "수집 실패";
        };
    }

    public String getStatusCssClass() {
        if (!active) {
            return "inactive";
        }

        if (latestSyncStatus == null) {
            return "wait";
        }

        return switch (latestSyncStatus) {
            case SUCCESS -> "success";
            case PARTIAL -> "warning";
            case FAIL -> "danger";
        };
    }
}