package com.seoulcareconnect.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

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
    private LocalDateTime createdAt;
    private List<String> photoUrls; // 신고 접수 시 첨부한 사진 (없으면 빈 리스트)
}
