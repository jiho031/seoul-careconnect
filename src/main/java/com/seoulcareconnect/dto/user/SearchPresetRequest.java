package com.seoulcareconnect.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SearchPresetRequest {

    @NotBlank(message = "프리셋 이름을 입력해주세요.")
    @Size(max = 30, message = "프리셋 이름은 30자 이내로 입력해주세요.")
    private String name;

    @Size(max = 120, message = "검색어는 120자 이내로 입력해주세요.")
    private String keyword;

    private String district;
    private String ageGroup;
    private String category;
    private String targetKeyword;
}
