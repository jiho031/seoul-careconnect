package com.seoulcareconnect.entity.report;

import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "missing_policy_reports",
        indexes = {
                @Index(name = "idx_report_status", columnList = "status"),
                @Index(name = "idx_report_type", columnList = "report_type"),
                @Index(name = "idx_report_created_at", columnList = "created_at")
        }
)
public class MissingPolicyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    // users.user_id 참조, 비회원 제보 허용 시 NULL
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // policies.policy_id 참조, 누락 제보의 경우 NULL
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private Policy policy;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "report_type", nullable = false, length = 30)
    private String reportType;
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 30)
    private String status = "RECEIVED";

    @Column(name = "admin_memo", length = 1000)
    private String adminMemo;

    // 첨부 사진 상대 경로(/uploads/reports/...)를 콤마로 이어붙여 저장
    @Lob
    @Column(name = "photo_urls", columnDefinition = "TEXT")
    private String photoUrls;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    public void prePersist() {
        if (status == null) status = "RECEIVED";
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    /**
     * 저장된 photoUrls(콤마 구분 문자열)를 URL 목록으로 변환한다.
     */
    public List<String> getPhotoUrlList() {
        if (photoUrls == null || photoUrls.isBlank()) {
            return new ArrayList<>();
        }

        return Arrays.stream(photoUrls.split(","))
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .toList();
    }

    public void setPhotoUrlList(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            this.photoUrls = null;
            return;
        }

        this.photoUrls = String.join(",", urls);
    }
}