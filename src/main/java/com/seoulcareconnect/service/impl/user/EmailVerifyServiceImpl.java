package com.seoulcareconnect.service.impl.user;

import com.seoulcareconnect.entity.user.EmailVerify;
import com.seoulcareconnect.repository.user.EmailVerifyRepository;
import com.seoulcareconnect.repository.user.UserRepository;
import com.seoulcareconnect.service.user.EmailVerifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerifyServiceImpl implements EmailVerifyService {

    private static final String PURPOSE_SIGNUP = "SIGNUP";

    private final EmailVerifyRepository emailVerifyRepository;
    private final UserRepository userRepository;
    private final JavaMailSender javaMailSender;

    @Override
    public void sendSignupCode(String email) {

        email = email.trim().toLowerCase();

        if (email.isBlank()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }


        String code = createCode();

        EmailVerify emailVerify = new EmailVerify();
        emailVerify.setEmail(email);
        emailVerify.setVerifyCode(code);
        emailVerify.setPurpose(PURPOSE_SIGNUP);
        emailVerify.setAttemptCount(0);
        emailVerify.setVerifiedYn(false);
        emailVerify.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        emailVerifyRepository.save(emailVerify);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[서울 케어넥트] 이메일 인증번호 안내");
        message.setText(
                "서울 케어넥트 회원가입 인증번호입니다.\n\n" +
                        "인증번호: " + code + "\n\n" +
                        "인증번호는 5분간 유효합니다."
        );

        javaMailSender.send(message);
    }

    @Override
    public void checkSignupCode(String email, String verifyCode) {

        EmailVerify emailVerify = emailVerifyRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, PURPOSE_SIGNUP)
                .orElseThrow(() -> new IllegalArgumentException("인증번호 발송 내역이 없습니다."));

        if (emailVerify.getVerifiedYn()) {
            return;
        }

        if (emailVerify.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("인증번호가 만료되었습니다.");
        }

        if (emailVerify.getAttemptCount() >= 5) {
            throw new IllegalArgumentException("인증 시도 횟수를 초과했습니다. 인증번호를 다시 발송해주세요.");
        }

        emailVerify.setAttemptCount(emailVerify.getAttemptCount() + 1);

        if (!emailVerify.getVerifyCode().equals(verifyCode)) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        emailVerify.setVerifiedYn(true);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailVerified(String email) {

        return emailVerifyRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, PURPOSE_SIGNUP)
                .filter(EmailVerify::getVerifiedYn)
                .filter(v -> v.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    private String createCode() {
        Random random = new Random();
        int number = 100000 + random.nextInt(900000);
        return String.valueOf(number);
    }
}