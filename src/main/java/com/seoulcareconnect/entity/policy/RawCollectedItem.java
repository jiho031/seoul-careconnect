package com.seoulcareconnect.entity.policy;

import com.seoulcareconnect.entity.policy.enums.RawType;
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
        name = "raw_collected_items",
        indexes = {
                @Index(name = "idx_raw_source_external", columnList = "source_id, external_id"),
                @Index(name = "idx_raw_content_hash", columnList = "content_hash")
        }
)
public class RawCollectedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "raw_id")
    private Long rawId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private PolicySource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "raw_type", nullable = false, length = 30)
    private RawType rawType = RawType.API;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Lob
    @Column(name = "raw_json", columnDefinition = "LONGTEXT")
    private String rawJson;

    @Lob
    @Column(name = "raw_xml", columnDefinition = "LONGTEXT")
    private String rawXml;

    @Lob
    @Column(name = "raw_text", columnDefinition = "LONGTEXT")
    private String rawText;


    @Lob
    @Column(name = "raw_html", columnDefinition = "LONGTEXT")
    private String rawHtml;

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @PrePersist
    public void prePersist() {
        if (rawType == null) rawType = RawType.API;
        if (collectedAt == null) collectedAt = LocalDateTime.now();
    }
}
