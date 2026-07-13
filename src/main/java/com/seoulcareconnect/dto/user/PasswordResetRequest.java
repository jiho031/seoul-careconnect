package com.seoulcareconnect.dto.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PasswordResetRequest {

    private String email;
    private String newPassword;
    private String newPasswordConfirm;
}
