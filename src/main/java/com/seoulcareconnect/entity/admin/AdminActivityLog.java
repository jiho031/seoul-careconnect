package com.seoulcareconnect.entity.admin;

import com.seoulcareconnect.entity.admin.enums.AdminActivityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "admin_activity_log",
        indexes = {
                @Index(
                        name = "idx_admin_activity_created_at",
                        columnList = "created_at"
                ),
                @Index(
                        name = "idx_admin_activity_admin_id",
                        columnList = "admin_id"
                ),
                @Index(
                        name = "idx_admin_activity_type",
                        columnList = "activity_type"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AdminActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_log_id")
    private Long activityLogId;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(
            name = "admin_name",
            nullable = false,
            length = 100
    )
    private String adminName;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "activity_type",
            nullable = false,
            length = 50
    )
    private AdminActivityType activityType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(
            name = "target_name",
            length = 300
    )
    private String targetName;

    @Column(
            name = "description",
            nullable = false,
            length = 1000
    )
    private String description;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (adminName == null || adminName.isBlank()) {
            adminName = "관리자";
        }
    }
}