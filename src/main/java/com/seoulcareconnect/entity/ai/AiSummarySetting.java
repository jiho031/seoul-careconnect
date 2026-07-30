package com.seoulcareconnect.entity.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "ai_summary_settings")
public class AiSummarySetting {

    public static final long DEFAULT_ID = 1L;

    @Id
    @Column(name = "setting_id")
    private Long settingId;

    @Column(name = "automatic_summary_enabled", nullable = false)
    private boolean automaticSummaryEnabled;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public static AiSummarySetting defaultSetting() {
        AiSummarySetting setting = new AiSummarySetting();
        setting.settingId = DEFAULT_ID;
        setting.automaticSummaryEnabled = false;
        return setting;
    }

    public void updateAutomaticSummaryEnabled(boolean enabled) {
        automaticSummaryEnabled = enabled;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
