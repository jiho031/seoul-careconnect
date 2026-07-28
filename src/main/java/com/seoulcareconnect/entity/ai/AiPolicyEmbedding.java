package com.seoulcareconnect.entity.ai;

import com.seoulcareconnect.entity.policy.Policy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "ai_policy_embeddings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_policy_embedding_policy",
                columnNames = "policy_id"
        )
)
public class AiPolicyEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "embedding_id")
    private Long embeddingId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "provider_name", length = 30)
    private String providerName;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(nullable = false)
    private Integer dimensions;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Lob
    @Column(name = "vector_json", nullable = false, columnDefinition = "LONGTEXT")
    private String vectorJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static AiPolicyEmbedding create(
            Policy policy,
            String providerName,
            String modelName,
            int dimensions,
            String contentHash,
            String vectorJson
    ) {
        AiPolicyEmbedding embedding = new AiPolicyEmbedding();
        embedding.policy = policy;
        embedding.update(providerName, modelName, dimensions, contentHash, vectorJson);
        return embedding;
    }

    public void update(
            String providerName,
            String modelName,
            int dimensions,
            String contentHash,
            String vectorJson
    ) {
        this.providerName = providerName;
        this.modelName = modelName;
        this.dimensions = dimensions;
        this.contentHash = contentHash;
        this.vectorJson = vectorJson;
    }

    public boolean matches(
            String providerName,
            String modelName,
            int dimensions,
            String contentHash
    ) {
        return Objects.equals(this.providerName, providerName)
                && Objects.equals(this.modelName, modelName)
                && this.dimensions == dimensions
                && this.contentHash.equals(contentHash);
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
