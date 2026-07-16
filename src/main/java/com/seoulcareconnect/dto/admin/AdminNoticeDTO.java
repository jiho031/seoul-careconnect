package com.seoulcareconnect.dto.admin;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class AdminNoticeDTO {

    private Long noticeId;

    private String title;
    private String content;
    private String noticeType;

    private Boolean pinned;

    private String writerName;
    private String writerEmail;

    private LocalDateTime createdAt;

    public String getNoticeTypeLabel() {
        if ("IMPORTANT".equalsIgnoreCase(noticeType)) {
            return "중요";
        }

        if ("SECURITY".equalsIgnoreCase(noticeType)) {
            return "보안";
        }

        return "안내";
    }

    public String getNoticeCssClass() {
        if ("IMPORTANT".equalsIgnoreCase(noticeType)) {
            return "important";
        }

        if ("SECURITY".equalsIgnoreCase(noticeType)) {
            return "security";
        }

        return "normal";
    }

    public String getCreatedAtText() {
        if (createdAt == null) {
            return "-";
        }

        return createdAt.format(
                DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
        );
    }

    public String getSafeWriterName() {
        if (writerName == null || writerName.isBlank()) {
            return "최고 관리자";
        }

        return writerName;
    }
}