package com.seoulcareconnect.service.user;

import com.seoulcareconnect.dto.user.SignupRequest;

public interface UserService {

    void signup(SignupRequest request);
}
