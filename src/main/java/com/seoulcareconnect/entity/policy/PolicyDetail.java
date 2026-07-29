package com.seoulcareconnect.entity.policy;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "policy_details")
public class PolicyDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Long detailId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false, unique = true)
    private Policy policy;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String benefit;

    @Lob
    @Column(name = "selection_criteria", columnDefinition = "TEXT")
    private String selectionCriteria;

    @Lob
    @Column(name = "required_documents_text", columnDefinition = "TEXT")
    private String requiredDocumentsText;

    @Lob
    @Column(name = "content_text", columnDefinition = "LONGTEXT")
    private String contentText;

    @Lob
    @Column(name = "content_html", columnDefinition = "LONGTEXT")
    private String contentHtml;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}