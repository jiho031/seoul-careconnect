package com.seoulcareconnect.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserUpdateRequest {

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 50, message = "이름은 50자 이내로 입력해주세요.")
    private String name;

    @Pattern(
            regexp = "\\d{4}",
            message = "출생연도는 숫자 4자리로 입력해주세요."
    )
    private String birthYear;

    @Pattern(
            regexp = "^$|^[0-9-]{9,13}$",
            message = "연락처 형식을 확인해주세요."
    )
    private String phone;

    private String ageGroup;

    @NotBlank(message = "관심 지역을 선택해주세요.")
    private String district;

    @NotBlank(message = "화면 모드를 선택해주세요.")
    private String uiMode;

    public static UserUpdateRequest from(MyPageResponse response) {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName(response.getName());
        request.setBirthYear(response.getBirthYear());
        request.setPhone(response.getPhone());
        request.setAgeGroup(response.getAgeGroup());
        request.setDistrict(response.getDistrict());
        request.setUiMode(response.getUiMode());
        return request;
    }
}
