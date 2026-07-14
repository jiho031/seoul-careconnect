package com.seoulcareconnect.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminPolicyExceptionSummaryDTO {

    // 관리자 확인이 필요한 전체 예외
    private long totalExceptionCount;

    // 필수값 누락
    private long missingRequiredFieldCount;

    // 시작일·종료일 등 기간 오류
    private long invalidDateCount;

    // 기존 정책과의 중복 후보
    private long duplicateCandidateCount;

    // 공식 URL 오류
    private long invalidUrlCount;

    // 아직 처리되지 않은 예외
    private long pendingCount;

    // 처리 중인 예외
    private long processingCount;

    // 처리 완료된 예외
    private long resolvedCount;
}