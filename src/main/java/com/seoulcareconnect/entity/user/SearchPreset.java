package com.seoulcareconnect.entity.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "policy_search_presets")
public class SearchPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long presetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(length = 120)
    private String keyword;

    @Column(length = 50)
    private String district;

    @Column(length = 20)
    private String ageGroup;

    @Column(length = 30)
    private String category;

    @Column(length = 50)
    private String targetKeyword;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private SearchPreset(User user, String name, String keyword, String district,
                         String ageGroup, String category, String targetKeyword) {
        this.user = user;
        this.name = name;
        this.keyword = keyword;
        this.district = district;
        this.ageGroup = ageGroup;
        this.category = category;
        this.targetKeyword = targetKeyword;
    }

    public static SearchPreset create(User user, String name, String keyword,
                                      String district, String ageGroup,
                                      String category, String targetKeyword) {
        return new SearchPreset(user, name, keyword, district, ageGroup, category, targetKeyword);
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
