package com.seoulcareconnect.entity.admin;

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
@Table(name = "admin_notices")
public class AdminNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noticeId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String content;

    /**
     * NORMAL    : 일반 안내
     * IMPORTANT : 중요 공지
     * SECURITY  : 보안 공지
     */
    @Column(nullable = false, length = 20)
    private String noticeType = "NORMAL";

    /**
     * 화면 공개 여부
     */
    @Column(nullable = false)
    private Boolean isVisible = true;

    /**
     * 상단 고정 여부
     */
    @Column(nullable = false)
    private Boolean isPinned = false;

    /**
     * 공지를 작성한 최고 관리자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();

        if (this.noticeType == null) {
            this.noticeType = "NORMAL";
        }

        if (this.isVisible == null) {
            this.isVisible = true;
        }

        if (this.isPinned == null) {
            this.isPinned = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}