package com.seoulcareconnect.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 마이페이지 "신고 내역" 카드에서 사용하는 DTO (FavoriteCardDto와 동일한 스타일)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissingPolicyReportCardDto {
    private Long reportId;
    private String title;
    private String reportType;
    private String reportTypeLabel;
    private String status;
    private String statusLabel;
    private String policyTitle; // 연결된 정책이 있으면 정책명, 없으면 null
    private String content;
    private String sourceUrl;
    private String adminMemo;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
