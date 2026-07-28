package com.seoulcareconnect.dto.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FavoriteDetailDto {
    private Long favoriteId;
    private Long policyId;

    private String title;
    private String category;
    private String summary;

    private String agency;
    private String regionDisplay;
    private String target;
    private String applyStatusLabel;
    private String applyPeriod;

    private LocalDateTime createdAt;
}
