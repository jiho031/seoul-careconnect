package com.seoulcareconnect.entity.report;

import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    public void prePersist() {
        if (status == null) status = "RECEIVED";
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}