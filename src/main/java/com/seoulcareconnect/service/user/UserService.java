package com.seoulcareconnect.service.user;

import com.seoulcareconnect.dto.user.MyPageResponse;
import com.seoulcareconnect.dto.user.PasswordChangeRequest;
import com.seoulcareconnect.dto.user.SignupRequest;
import com.seoulcareconnect.dto.user.UserUpdateRequest;
import com.seoulcareconnect.dto.user.WithdrawRequest;
import org.springframework.security.core.Authentication;

public interface UserService {

    void signup(SignupRequest request);

    MyPageResponse getMyPage(Authentication authentication);

    void updateMyPage(
            Authentication authentication,
            UserUpdateRequest request
    );

    void changePassword(
            Authentication authentication,
            PasswordChangeRequest request
    );

    void withdraw(
            Authentication authentication,
            WithdrawRequest request
    );
}
