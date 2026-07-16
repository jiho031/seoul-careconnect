package com.seoulcareconnect.service.user;

public interface PasswordResetService {

    void resetPassword(
            String email,
            String newPassword,
            String newPasswordConfirm
    );
}
