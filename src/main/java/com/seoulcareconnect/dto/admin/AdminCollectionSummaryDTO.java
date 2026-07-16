package com.seoulcareconnect.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminCollectionSummaryDTO {

    // 가장 최근 수집 1회의 처리 건수
    private long latestProcessedCount;

    // 가장 최근 수집 1회의 성공 건수
    private long latestSuccessCount;

    // 가장 최근 수집 1회의 실패 건수
    private long latestFailCount;

    // 오늘 실행된 수집 로그 수
    private long todayRunCount;

    private long totalSourceCount;
    private long activeSourceCount;
    private long totalPolicyCount;
}