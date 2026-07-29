package com.seoulcareconnect.dto.admin;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class AdminRecentUserDTO {

    private Long userId;
    private String name;
    private String email;
    private String role;
    private Boolean active;
    private LocalDateTime createdAt;

    public String getRoleLabel() {
        if (role == null) {
            return "일반 회원";
        }

        return switch (role.toUpperCase()) {
            case "SUPER_ADMIN" -> "최고 관리자";
            case "ADMIN" -> "관리자";
            default -> "일반 회원";
        };
    }

    public String getStatusLabel() {
        return Boolean.TRUE.equals(active)
                ? "활성"
                : "비활성";
    }

    public String getStatusCssClass() {
        return Boolean.TRUE.equals(active)
                ? "active"
                : "inactive";
    }

    public String getCreatedAtText() {
        if (createdAt == null) {
            return "-";
        }

        return createdAt.format(
                DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
        );
    }
}