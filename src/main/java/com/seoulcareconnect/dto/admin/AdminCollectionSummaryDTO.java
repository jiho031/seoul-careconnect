package com.seoulcareconnect.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminCollectionSummaryDTO {

    // 오늘 API에서 받아온 전체 항목 수
    private long todayRequestedCount;

    // 오늘 정상적으로 정책 저장 또는 수정된 항목 수
    private long todaySuccessCount;

    // 관리자 확인이 필요한 항목 수
    private long reviewRequiredCount;

    // 오늘 수집에 실패한 항목 수
    private long todayFailCount;

    // 등록된 전체 API 출처 수
    private long totalSourceCount;

    // 현재 활성화된 API 출처 수
    private long activeSourceCount;

    // 현재 등록된 전체 정책 수
    private long totalPolicyCount;
}