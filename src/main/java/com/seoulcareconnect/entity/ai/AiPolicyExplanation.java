package com.seoulcareconnect.entity.ai;

import com.seoulcareconnect.entity.policy.Policy;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "ai_policy_explanations",
        indexes = {
                @Index(name = "idx_ai_explanation_policy", columnList = "policy_id"),
                @Index(name = "idx_ai_explanation_status", columnList = "review_status"),
                @Index(name = "idx_ai_explanation_created", columnList = "created_at")
        }
)
public class AiPolicyExplanation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "explanation_id")
    private Long explanationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 30)
    private ReviewStatus reviewStatus = ReviewStatus.DRAFT;

    @Lob
    @Column(name = "easy_summary", nullable = false, columnDefinition = "TEXT")
    private String easySummary;

    @Lob
    @Column(name = "eligibility_summary", nullable = false, columnDefinition = "TEXT")
    private String eligibilitySummary;

    @Lob
    @Column(name = "benefit_summary", nullable = false, columnDefinition = "TEXT")
    private String benefitSummary;

    @Lob
    @Column(name = "application_summary", nullable = false, columnDefinition = "TEXT")
    private String applicationSummary;

    @Lob
    @Column(name = "caution_summary", nullable = false, columnDefinition = "TEXT")
    private String cautionSummary;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "prompt_version", nullable = false, length = 100)
    private String promptVersion;

    @Lob
    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "reviewed_by", length = 200)
    private String reviewedBy;

    @Column(name = "source_updated_at")
    private LocalDateTime sourceUpdatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public static AiPolicyExplanation draft(
            Policy policy,
            Content content,
            String modelName,
            String promptVersion
    ) {
        AiPolicyExplanation explanation = new AiPolicyExplanation();
        explanation.policy = policy;
        explanation.easySummary = content.easySummary();
        explanation.eligibilitySummary = content.eligibilitySummary();
        explanation.benefitSummary = content.benefitSummary();
        explanation.applicationSummary = content.applicationSummary();
        explanation.cautionSummary = content.cautionSummary();
        explanation.modelName = modelName;
        explanation.promptVersion = promptVersion;
        explanation.sourceUpdatedAt = policy.getUpdatedAt() != null
                ? policy.getUpdatedAt()
                : policy.getCreatedAt();
        explanation.reviewStatus = ReviewStatus.DRAFT;
        return explanation;
    }

    public static AiPolicyExplanation generated(
            Policy policy,
            Content content,
            String modelName,
            String promptVersion
    ) {
        AiPolicyExplanation explanation = draft(policy, content, modelName, promptVersion);
        explanation.reviewStatus = ReviewStatus.APPROVED;
        return explanation;
    }

    public void updateContent(Content content, String editor) {
        easySummary = content.easySummary();
        eligibilitySummary = content.eligibilitySummary();
        benefitSummary = content.benefitSummary();
        applicationSummary = content.applicationSummary();
        cautionSummary = content.cautionSummary();
        reviewedBy = normalize(editor);
        reviewedAt = LocalDateTime.now();
    }

    public void approve(String reviewer, String comment) {
        reviewStatus = ReviewStatus.APPROVED;
        reviewedBy = reviewer;
        reviewComment = normalize(comment);
        reviewedAt = LocalDateTime.now();
    }

    public void reject(String reviewer, String comment) {
        reviewStatus = ReviewStatus.REJECTED;
        reviewedBy = reviewer;
        reviewComment = normalize(comment);
        reviewedAt = LocalDateTime.now();
    }

    public void supersede() {
        reviewStatus = ReviewStatus.SUPERSEDED;
        updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (reviewStatus == null) reviewStatus = ReviewStatus.DRAFT;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Getter
    @RequiredArgsConstructor
    public enum ReviewStatus {
        DRAFT("검수 대기"),
        APPROVED("생성 완료"),
        REJECTED("반려"),
        SUPERSEDED("이전 버전");

        private final String label;
    }

    public record Content(
            String easySummary,
            String eligibilitySummary,
            String benefitSummary,
            String applicationSummary,
            String cautionSummary
    ) {
    }
}
