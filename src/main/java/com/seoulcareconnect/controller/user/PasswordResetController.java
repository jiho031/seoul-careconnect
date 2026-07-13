package com.seoulcareconnect.controller.user;

import com.seoulcareconnect.dto.user.EmailCheckRequest;
import com.seoulcareconnect.dto.user.EmailSendRequest;
import com.seoulcareconnect.dto.user.PasswordResetRequest;
import com.seoulcareconnect.service.user.EmailVerifyService;
import com.seoulcareconnect.service.user.PasswordResetService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private static final String SESSION_RESET_EMAIL = "PASSWORD_RESET_EMAIL";

    private final EmailVerifyService emailVerifyService;
    private final PasswordResetService passwordResetService;

    @GetMapping("/password/find")
    public String passwordFindPage(
            HttpSession session,
            Model model
    ) {
        String verifiedEmail = (String) session.getAttribute(SESSION_RESET_EMAIL);

        boolean resetVerified = verifiedEmail != null
                && emailVerifyService.isPasswordResetVerified(verifiedEmail);

        if (!resetVerified) {
            session.removeAttribute(SESSION_RESET_EMAIL);
            verifiedEmail = "";
        }

        model.addAttribute("resetVerified", resetVerified);
        model.addAttribute("verifiedEmail", verifiedEmail);

        return "auth/password-find";
    }

    @PostMapping("/password/email/send")
    @ResponseBody
    public ResponseEntity<?> sendPasswordResetCode(
            @RequestBody EmailSendRequest request,
            HttpSession session
    ) {
        try {
            session.removeAttribute(SESSION_RESET_EMAIL);
            emailVerifyService.sendPasswordResetCode(request.getEmail());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "비밀번호 재설정 인증번호가 발송되었습니다."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", safeMessage(e)
            ));
        }
    }

    @PostMapping("/password/email/check")
    @ResponseBody
    public ResponseEntity<?> checkPasswordResetCode(
            @RequestBody EmailCheckRequest request,
            HttpSession session
    ) {
        try {
            emailVerifyService.checkPasswordResetCode(
                    request.getEmail(),
                    request.getVerifyCode()
            );

            String normalizedEmail = normalizeEmail(request.getEmail());
            session.setAttribute(SESSION_RESET_EMAIL, normalizedEmail);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "이메일 인증이 완료되었습니다."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", safeMessage(e)
            ));
        }
    }

    @PostMapping("/password/reset")
    public String resetPassword(
            PasswordResetRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String verifiedEmail = (String) session.getAttribute(SESSION_RESET_EMAIL);

            if (verifiedEmail == null
                    || !emailVerifyService.isPasswordResetVerified(verifiedEmail)) {
                throw new IllegalArgumentException("이메일 인증이 만료되었습니다. 다시 인증해주세요.");
            }

            String requestEmail = normalizeEmail(request.getEmail());

            if (!verifiedEmail.equals(requestEmail)) {
                throw new IllegalArgumentException("인증한 이메일 정보가 일치하지 않습니다.");
            }

            passwordResetService.resetPassword(
                    verifiedEmail,
                    request.getNewPassword(),
                    request.getNewPasswordConfirm()
            );

            session.removeAttribute(SESSION_RESET_EMAIL);

            return "redirect:/login?reset=true";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "resetError",
                    safeMessage(e)
            );

            return "redirect:/password/find";
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일 정보가 없습니다.");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String safeMessage(IllegalArgumentException e) {
        return e.getMessage() == null
                ? "요청 처리 중 오류가 발생했습니다."
                : e.getMessage();
    }
}
