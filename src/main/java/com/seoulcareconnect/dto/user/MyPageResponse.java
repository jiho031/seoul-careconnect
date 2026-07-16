package com.seoulcareconnect.dto.user;

import com.seoulcareconnect.entity.user.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class MyPageResponse {

    private Long userId;
    private String email;
    private String name;
    private String birthYear;
    private String phone;
    private String ageGroup;
    private String region;
    private String district;
    private String uiMode;
    private String provider;
    private LocalDateTime createdAt;

    public static MyPageResponse from(User user) {
        return MyPageResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .birthYear(user.getBirthYear())
                .phone(user.getPhone())
                .ageGroup(user.getAgeGroup())
                .region(user.getRegion())
                .district(user.getDistrict())
                .uiMode(user.getUiMode())
                .provider(user.getProvider())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public String getProviderLabel() {
        if (provider == null) {
            return "이메일 가입";
        }

        return switch (provider.toUpperCase()) {
            case "GOOGLE" -> "구글 로그인";
            case "KAKAO" -> "카카오 로그인";
            default -> "이메일 가입";
        };
    }

    public String getCreatedAtText() {
        if (createdAt == null) {
            return "-";
        }
        return createdAt.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
    }

    public String getInitial() {
        if (name == null || name.isBlank()) {
            return "서";
        }
        return name.substring(0, 1);
    }

    public boolean isLocalAccount() {
        return provider == null || "LOCAL".equalsIgnoreCase(provider);
    }
}
