package com.seoulcareconnect.entity.ai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AiReviewStatus {
    DRAFT("검수 대기"),
    APPROVED("승인"),
    REJECTED("반려"),
    SUPERSEDED("이전 버전");

    private final String label;
}
