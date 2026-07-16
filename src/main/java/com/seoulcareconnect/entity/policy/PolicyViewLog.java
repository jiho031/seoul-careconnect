package com.seoulcareconnect.entity.policy;

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
        name = "policy_view_logs",
        indexes = {
                @Index(name = "idx_policy_view_user", columnList = "user_id"),
                @Index(name = "idx_policy_view_policy", columnList = "policy_id"),
                @Index(name = "idx_policy_viewed_at", columnList = "viewed_at")
        }
)
public class PolicyViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "view_log_id")
    private Long viewLogId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @PrePersist
    public void prePersist() {
        if (viewedAt == null) viewedAt = LocalDateTime.now();
    }
}
