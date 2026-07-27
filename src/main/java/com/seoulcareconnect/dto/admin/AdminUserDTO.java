package com.seoulcareconnect.dto.admin;

import com.seoulcareconnect.entity.user.User;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
public class AdminUserDTO {

    private final Long userId;
    private final String name;
    private final String email;
    private final String maskedEmail;
    private final String phone;
    private final String provider;

    private final String role;
    private final String roleLabel;
    private final String roleCssClass;

    private final String ageGroup;
    private final String ageGroupLabel;

    private final String region;
    private final String district;
    private final String regionText;

    private final Boolean active;
    private final String statusLabel;
    private final String statusCssClass;

    private final LocalDateTime createdAt;
    private final String createdAtText;

    private AdminUserDTO(
            Long userId,
            String name,
            String email,
            String maskedEmail,
            String phone,
            String provider,
            String role,
            String roleLabel,
            String roleCssClass,
            String ageGroup,
            String ageGroupLabel,
            String region,
            String district,
            String regionText,
            Boolean active,
            String statusLabel,
            String statusCssClass,
            LocalDateTime createdAt,
            String createdAtText
    ) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.maskedEmail = maskedEmail;
        this.phone = phone;
        this.provider = provider;
        this.role = role;
        this.roleLabel = roleLabel;
        this.roleCssClass = roleCssClass;
        this.ageGroup = ageGroup;
        this.ageGroupLabel = ageGroupLabel;
        this.region = region;
        this.district = district;
        this.regionText = regionText;
        this.active = active;
        this.statusLabel = statusLabel;
        this.statusCssClass = statusCssClass;
        this.createdAt = createdAt;
        this.createdAtText = createdAtText;
    }

    public static AdminUserDTO from(User user) {
        return new AdminUserDTO(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                maskEmail(user.getEmail()),
                user.getPhone(),
                resolveProvider(user.getProvider()),

                user.getRole(),
                resolveRoleLabel(user.getRole()),
                resolveRoleCssClass(user.getRole()),

                user.getAgeGroup(),
                resolveAgeGroupLabel(user.getAgeGroup()),

                user.getRegion(),
                user.getDistrict(),
                resolveRegionText(
                        user.getRegion(),
                        user.getDistrict()
                ),

                user.getIsActive(),
                Boolean.TRUE.equals(user.getIsActive())
                        ? "활성"
                        : "비활성",
                Boolean.TRUE.equals(user.getIsActive())
                        ? "ok"
                        : "bad",

                user.getCreatedAt(),
                formatDateTime(user.getCreatedAt())
        );
    }

    private static String resolveProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "LOCAL";
        }

        return provider;
    }

    private static String resolveRoleLabel(String role) {
        if (role == null) {
            return "일반 회원";
        }

        return switch (role) {
            case "ADMIN" -> "관리자";
            case "SUPER_ADMIN" -> "최고 관리자";
            default -> "일반 회원";
        };
    }

    private static String resolveRoleCssClass(String role) {
        if (role == null) {
            return "user";
        }

        return switch (role) {
            case "ADMIN" -> "admin";
            case "SUPER_ADMIN" -> "super";
            default -> "user";
        };
    }

    private static String resolveAgeGroupLabel(String ageGroup) {
        if (ageGroup == null || ageGroup.isBlank()) {
            return "-";
        }

        return switch (ageGroup) {
            case "UNDER_40" -> "30대 이하";
            case "FORTIES" -> "40대";
            case "FIFTIES" -> "50대";
            case "SIXTIES_PLUS" -> "60대 이상";
            case "ALL" -> "전 연령";
            default -> ageGroup;
        };
    }

    private static String resolveRegionText(
            String region,
            String district
    ) {
        boolean hasRegion =
                region != null && !region.isBlank();

        boolean hasDistrict =
                district != null && !district.isBlank();

        if (hasRegion && hasDistrict) {
            return region + " " + district;
        }

        if (hasDistrict) {
            return district;
        }

        if (hasRegion) {
            return region;
        }

        return "-";
    }

    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "-";
        }

        int atIndex = email.indexOf("@");

        if (atIndex <= 0) {
            return email;
        }

        String localPart =
                email.substring(0, atIndex);

        String domain =
                email.substring(atIndex);

        if (localPart.length() <= 2) {
            return localPart.charAt(0)
                    + "***"
                    + domain;
        }

        return localPart.substring(0, 2)
                + "***"
                + domain;
    }

    private static String formatDateTime(
            LocalDateTime dateTime
    ) {
        if (dateTime == null) {
            return "-";
        }

        return dateTime.format(
                DateTimeFormatter.ofPattern("yyyy.MM.dd")
        );
    }
}