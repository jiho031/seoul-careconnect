package com.seoulcareconnect.entity.policy;

import com.seoulcareconnect.entity.policy.enums.DocumentCategory;
import com.seoulcareconnect.entity.policy.enums.DocumentRequirementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "policy_documents",
        indexes = {
                @Index(name = "idx_policy_document_policy", columnList = "policy_id"),
                @Index(name = "idx_policy_document_guide", columnList = "guide_id"),
                @Index(name = "idx_policy_document_check_key", columnList = "checklist_key")
        }
)
public class PolicyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_document_id")
    private Long policyDocumentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id")
    private DocumentGuide guide;

    @Lob
    @Column(name = "original_text", nullable = false, columnDefinition = "TEXT")
    private String originalText;

    @Column(name = "display_name", nullable = false, length = 300)
    private String displayName;

    @Column(name = "normalized_key", nullable = false, length = 180)
    private String normalizedKey;

    @Column(name = "checklist_key", nullable = false, length = 220)
    private String checklistKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_type", nullable = false, length = 30)
    private DocumentRequirementType requirementType;

    @Column(name = "condition_text", length = 500)
    private String conditionText;

    @Column(name = "alternative_group", length = 100)
    private String alternativeGroup;

    @Column(name = "source_type", length = 40)
    private String sourceType;

    @Column(name = "attachment_name", length = 500)
    private String attachmentName;

    @Column(name = "download_url", length = 1000)
    private String downloadUrl;

    @Column(name = "official_page_url", length = 1000)
    private String officialPageUrl;

    @Column(nullable = false)
    private Integer confidence;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (category == null) category = DocumentCategory.OTHER;
        if (requirementType == null) requirementType = DocumentRequirementType.REQUIRED;
        if (confidence == null) confidence = 50;
        if (sortOrder == null) sortOrder = 0;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
