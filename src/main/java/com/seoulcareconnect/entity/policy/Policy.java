package com.seoulcareconnect.entity.policy;

import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.entity.policy.enums.PolicyStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "policies",
        indexes = {
                @Index(name = "idx_policy_status", columnList = "status"),
                @Index(name = "idx_policy_apply_status", columnList = "apply_status"),
                @Index(name = "idx_policy_category", columnList = "category"),
                @Index(name = "idx_policy_district", columnList = "district"),
                @Index(name = "idx_policy_end_date", columnList = "end_date"),
                @Index(name = "idx_policy_created_at", columnList = "created_at"),
                @Index(name = "idx_policy_view_count", columnList = "view_count")
        }
)
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long policyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private PolicySource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_id")
    private RawCollectedItem rawItem;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(nullable = false, length = 300)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PolicyCategory category;

    @Column(length = 300)
    private String target;

    @Column(length = 50)
    private String region;

    @Column(length = 50)
    private String district;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "apply_status", nullable = false, length = 30)
    private ApplyStatus applyStatus = ApplyStatus.OPEN;

    @Column(name = "apply_method", length = 500)
    private String applyMethod;

    @Column(name = "official_url", length = 1000)
    private String officialUrl;

    @Column(length = 200)
    private String contact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PolicyStatus status = PolicyStatus.PENDING_REVIEW;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @OneToOne(
            mappedBy = "policy",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private PolicyDetail detail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void attachDetail(PolicyDetail detail) {
        this.detail = detail;
        if (detail != null) detail.setPolicy(this);
    }

    @PrePersist
    public void prePersist() {
        if (category == null) category = PolicyCategory.LIVING_SUPPORT;
        if (applyStatus == null) applyStatus = ApplyStatus.OPEN;
        if (status == null) status = PolicyStatus.PENDING_REVIEW;
        if (viewCount == null) viewCount = 0;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
