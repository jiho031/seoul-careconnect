package com.seoulcareconnect.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawRequest {

    private String currentPassword;

    @NotBlank(message = "확인 문구를 입력해주세요.")
    private String confirmText;
}
