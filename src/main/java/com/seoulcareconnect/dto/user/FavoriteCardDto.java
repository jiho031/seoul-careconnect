// 실제 프로젝트 경로: src/main/java/com/seoulcareconnect/dto/user/FavoriteCardDto.java
// [추가] deleted 필드 하나만 새로 추가했습니다. 나머지는 기존 그대로입니다.
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
    private String summary;
    private String target;
    private String ageGroupDisplay;
    private String regionDisplay;
    private String applyStatusLabel;
    private String applyPeriod;
    private String dDayLabel;
    private String dDayCssClass;
    private LocalDateTime favoritedAt;

    // [추가] 정책이 삭제되어 더 이상 조회되지 않는 카드인지 여부
    // (list.html에서 클릭 시 에러 페이지 대신 안내 모달을 띄우는 데 사용)
    private boolean deleted;
}
