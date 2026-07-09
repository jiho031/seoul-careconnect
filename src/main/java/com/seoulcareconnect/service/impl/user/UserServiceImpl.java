package com.seoulcareconnect.service.impl.user;

import com.seoulcareconnect.dto.user.SignupRequest;
import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.user.UserRepository;
import com.seoulcareconnect.service.user.EmailVerifyService;
import com.seoulcareconnect.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerifyService emailVerifyService;

    @Override
    public void signup(SignupRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        if (!emailVerifyService.isEmailVerified(email)) {
            throw new IllegalArgumentException("이메일 인증을 완료해주세요.");
        }

        User user = new User();
        user.setBirthYear(request.getBirthYear());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setAgeGroup(request.getAgeGroup());
        user.setDistrict(request.getDistrict());

        user.setRole("USER");
        user.setRegion("서울");

        if ("60대 이상".equals(request.getAgeGroup())) {
            user.setUiMode("LARGE_TEXT");
        } else {
            user.setUiMode("DEFAULT");
        }

        userRepository.save(user);
    }
}