package com.seoulcareconnect.service.impl.user;

import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.user.UserRepository;
import com.seoulcareconnect.service.user.EmailVerifyService;
import com.seoulcareconnect.service.user.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerifyService emailVerifyService;

    @Override
    public void resetPassword(
            String email,
            String newPassword,
            String newPasswordConfirm
    ) {
        String normalizedEmail = normalizeEmail(email);

        if (!emailVerifyService.isPasswordResetVerified(normalizedEmail)) {
            throw new IllegalArgumentException("이메일 인증을 다시 완료해주세요.");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("가입된 회원을 찾을 수 없습니다."));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        }

        validateLocalAccount(user);
        validatePassword(newPassword, newPasswordConfirm);

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호와 다른 비밀번호를 입력해주세요.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        emailVerifyService.consumePasswordResetVerification(normalizedEmail);
    }

    private void validateLocalAccount(User user) {
        String provider = user.getProvider();

        // 기존 일반 회원 데이터에 provider가 없을 수 있으므로 null/공백은 LOCAL로 처리
        if (provider != null
                && !provider.isBlank()
                && !"LOCAL".equalsIgnoreCase(provider)) {
            throw new IllegalArgumentException(
                    "소셜 로그인 계정은 해당 소셜 서비스에서 비밀번호를 변경해주세요."
            );
        }
    }

    private void validatePassword(
            String newPassword,
            String newPasswordConfirm
    ) {
        if (newPassword == null ||
                !newPassword.matches(
                        "^(?=\\S{8,20}$)"
                                + "(?=.*[A-Za-z])"
                                + "(?=.*\\d)"
                                + "(?=.*[!@#$%^&*]).*$"
                )) {
            throw new IllegalArgumentException(
                    "비밀번호는 8~20자이며 영문, 숫자, "
                            + "특수문자(!@#$%^&*)를 각각 1개 이상 포함해야 합니다."
            );
        }

        if (!newPassword.equals(newPasswordConfirm)) {
            throw new IllegalArgumentException(
                    "새 비밀번호와 확인값이 일치하지 않습니다."
            );
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일 정보가 없습니다.");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}
