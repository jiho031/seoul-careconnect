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
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("새 비밀번호를 입력해주세요.");
        }

        if (!newPassword.equals(newPasswordConfirm)) {
            throw new IllegalArgumentException("새 비밀번호와 확인값이 일치하지 않습니다.");
        }

        if (newPassword.length() < 8 || newPassword.length() > 20) {
            throw new IllegalArgumentException("비밀번호는 8자 이상 20자 이하로 입력해주세요.");
        }

        boolean hasLetter = newPassword.chars()
                .anyMatch(Character::isLetter);
        boolean hasDigit = newPassword.chars()
                .anyMatch(Character::isDigit);
        boolean hasSpecial = newPassword.chars()
                .anyMatch(ch -> !Character.isLetterOrDigit(ch)
                        && !Character.isWhitespace(ch));
        boolean hasWhitespace = newPassword.chars()
                .anyMatch(Character::isWhitespace);

        if (!hasLetter || !hasDigit || !hasSpecial || hasWhitespace) {
            throw new IllegalArgumentException(
                    "비밀번호는 공백 없이 영문, 숫자, 특수문자를 포함해야 합니다."
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
