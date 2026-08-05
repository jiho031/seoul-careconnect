package com.seoulcareconnect.entity.policy;

import com.seoulcareconnect.entity.policy.enums.DocumentCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "document_guides",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_document_guide_key",
                columnNames = "guide_key"
        )
)
public class DocumentGuide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guide_id")
    private Long guideId;

    @Column(name = "guide_key", nullable = false, length = 100)
    private String guideKey;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentCategory category;

    @Column(length = 200)
    private String issuer;

    @Column(name = "official_url", length = 1000)
    private String officialUrl;

    @Column(name = "help_name", length = 200)
    private String helpName;

    @Column(name = "help_url", length = 1000)
    private String helpUrl;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String summary;

    @Lob
    @Column(name = "steps_text", columnDefinition = "TEXT")
    private String stepsText;

    @Lob
    @Column(name = "preparation_text", columnDefinition = "TEXT")
    private String preparationText;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String caution;

    @Lob
    @Column(name = "aliases_text", nullable = false, columnDefinition = "TEXT")
    private String aliasesText;

    @Column(name = "verified_on")
    private LocalDate verifiedOn;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (category == null) category = DocumentCategory.OTHER;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
