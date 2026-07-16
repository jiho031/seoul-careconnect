package com.seoulcareconnect.dto.user;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Getter
@Setter
public class SignupRequest {

    private String email;
    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Pattern(
            regexp = "^(?=\\S{8,20}$)(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*]).*$",
            message = "비밀번호는 8~20자이며 영문, 숫자, 특수문자(!@#$%^&*)를 각각 1개 이상 포함해야 합니다."
    )
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    private String passwordConfirm;
    private String name;
    private String phone;
    private String ageGroup;
    private String district;
    private String birthYear;
}