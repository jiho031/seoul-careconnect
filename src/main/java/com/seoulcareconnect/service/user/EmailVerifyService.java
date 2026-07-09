package com.seoulcareconnect.service.user;

public interface EmailVerifyService {

    void sendSignupCode(String email);

    void checkSignupCode(String email, String verifyCode);

    boolean isEmailVerified(String email);
}