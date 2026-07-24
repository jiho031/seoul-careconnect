package com.seoulcareconnect.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteCardDto {
    private Long favoriteId;
    private Long policyId;
    private String title;
    private String categoryLabel;
    private String agency;
    private String summary;
    private String target;
    private String ageGroupDisplay;
    private String regionDisplay;
    private String applyStatusLabel;
    private String applyPeriod;
    private String dDayLabel;
    private String dDayCssClass;
    private LocalDateTime favoritedAt;
}