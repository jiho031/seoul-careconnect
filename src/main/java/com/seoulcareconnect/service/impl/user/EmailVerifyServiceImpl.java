package com.seoulcareconnect.service.impl.user;

import com.seoulcareconnect.entity.user.EmailVerify;
import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.user.EmailVerifyRepository;
import com.seoulcareconnect.repository.user.UserRepository;
import com.seoulcareconnect.service.user.EmailVerifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerifyServiceImpl implements EmailVerifyService {

    private static final String PURPOSE_SIGNUP = "SIGNUP";
    private static final String PURPOSE_PASSWORD_RESET = "PASSWORD_RESET";
    private static final int CODE_EXPIRATION_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerifyRepository emailVerifyRepository;
    private final UserRepository userRepository;
    private final JavaMailSender javaMailSender;

    @Override
    public void sendSignupCode(String email) {
        String normalizedEmail = normalizeEmail(email);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        createAndSendCode(
                normalizedEmail,
                PURPOSE_SIGNUP,
                "[서울 케어넥트] 회원가입 이메일 인증번호",
                "서울 케어넥트 회원가입 인증번호입니다."
        );
    }

    @Override
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void checkSignupCode(String email, String verifyCode) {
        checkCode(email, verifyCode, PURPOSE_SIGNUP);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailVerified(String email) {
        return isVerified(email, PURPOSE_SIGNUP);
    }

    @Override
    public void sendPasswordResetCode(String email) {
        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("가입된 이메일이 없습니다."));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        }

        String provider = user.getProvider();
        if (provider != null
                && !provider.isBlank()
                && !"LOCAL".equalsIgnoreCase(provider)) {
            throw new IllegalArgumentException(
                    "소셜 로그인 계정은 해당 소셜 서비스에서 비밀번호를 변경해주세요."
            );
        }

        createAndSendCode(
                normalizedEmail,
                PURPOSE_PASSWORD_RESET,
                "[서울 케어넥트] 비밀번호 재설정 인증번호",
                "서울 케어넥트 비밀번호 재설정 인증번호입니다."
        );
    }

    @Override
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void checkPasswordResetCode(String email, String verifyCode) {
        checkCode(email, verifyCode, PURPOSE_PASSWORD_RESET);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPasswordResetVerified(String email) {
        return isVerified(email, PURPOSE_PASSWORD_RESET);
    }

    @Override
    public void consumePasswordResetVerification(String email) {
        String normalizedEmail = normalizeEmail(email);

        EmailVerify emailVerify = emailVerifyRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(
                        normalizedEmail,
                        PURPOSE_PASSWORD_RESET
                )
                .orElseThrow(() -> new IllegalArgumentException("비밀번호 재설정 인증 내역이 없습니다."));

        emailVerify.setVerifiedYn(false);
        emailVerify.setExpiresAt(LocalDateTime.now());
    }

    private void createAndSendCode(
            String email,
            String purpose,
            String subject,
            String introText
    ) {
        String code = createCode();

        EmailVerify emailVerify = new EmailVerify();
        emailVerify.setEmail(email);
        emailVerify.setVerifyCode(code);
        emailVerify.setPurpose(purpose);
        emailVerify.setAttemptCount(0);
        emailVerify.setVerifiedYn(false);
        emailVerify.setExpiresAt(
                LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES)
        );

        emailVerifyRepository.save(emailVerify);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText(
                introText + "\n\n" +
                        "인증번호: " + code + "\n\n" +
                        "인증번호는 " + CODE_EXPIRATION_MINUTES + "분간 유효합니다."
        );

        javaMailSender.send(message);
    }

    private void checkCode(
            String email,
            String verifyCode,
            String purpose
    ) {
        String normalizedEmail = normalizeEmail(email);

        if (verifyCode == null || verifyCode.isBlank()) {
            throw new IllegalArgumentException("인증번호를 입력해주세요.");
        }

        EmailVerify emailVerify = emailVerifyRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(
                        normalizedEmail,
                        purpose
                )
                .orElseThrow(() -> new IllegalArgumentException("인증번호 발송 내역이 없습니다."));

        if (Boolean.TRUE.equals(emailVerify.getVerifiedYn())) {
            return;
        }

        if (emailVerify.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("인증번호가 만료되었습니다. 다시 발송해주세요.");
        }

        int attemptCount = emailVerify.getAttemptCount() == null
                ? 0
                : emailVerify.getAttemptCount();

        if (attemptCount >= MAX_ATTEMPTS) {
            throw new IllegalArgumentException(
                    "인증 시도 횟수를 초과했습니다. 인증번호를 다시 발송해주세요."
            );
        }

        emailVerify.setAttemptCount(attemptCount + 1);

        if (!emailVerify.getVerifyCode().equals(verifyCode.trim())) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        emailVerify.setVerifiedYn(true);
    }

    private boolean isVerified(String email, String purpose) {
        if (email == null || email.isBlank()) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        return emailVerifyRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(
                        normalizedEmail,
                        purpose
                )
                .filter(v -> Boolean.TRUE.equals(v.getVerifiedYn()))
                .filter(v -> v.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    private String createCode() {
        int number = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(number);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}
