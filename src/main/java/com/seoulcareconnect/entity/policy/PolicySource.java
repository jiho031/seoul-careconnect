package com.seoulcareconnect.entity.policy;

import com.seoulcareconnect.entity.policy.enums.SourceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "policy_sources", uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_policy_source_name",
                        columnNames = "source_name"
                )
        })
public class PolicySource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private SourceType sourceType = SourceType.OPEN_API;

    @Column(name = "base_url", length = 1000)
    private String baseUrl;

    @Column(name = "api_key_type", length = 50)
    private String apiKeyType;

    @Column(length = 50)
    private String category;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (sourceType == null) sourceType = SourceType.OPEN_API;
        if (isActive == null) isActive = true;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
