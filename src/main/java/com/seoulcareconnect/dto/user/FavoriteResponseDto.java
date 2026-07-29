package com.seoulcareconnect.dto.user;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class FavoriteResponseDto {
    private Long id;
    private Long policyId;
    private LocalDateTime createdAt;
}