package com.seoulcareconnect.entity.policy;

import com.seoulcareconnect.entity.policy.enums.PolicyErrorStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyErrorType;
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
        name = "policy_collection_errors",
        indexes = {
                @Index(name = "idx_policy_error_type", columnList = "error_type"),
                @Index(name = "idx_policy_error_status", columnList = "status"),
                @Index(name = "idx_policy_error_created_at", columnList = "created_at")
        }
)
public class PolicyCollectionError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "error_id")
    private Long errorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_id")
    private RawCollectedItem rawItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private PolicySource source;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "policy_title", length = 300)
    private String policyTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_type", nullable = false, length = 30)
    private PolicyErrorType errorType;

    @Column(name = "error_message", nullable = false, length = 1000)
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PolicyErrorStatus status = PolicyErrorStatus.WAITING;

    @Column(name = "admin_memo", length = 1000)
    private String adminMemo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = PolicyErrorStatus.WAITING;
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}