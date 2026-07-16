package com.seoulcareconnect.controller.user;

import com.seoulcareconnect.dto.user.EmailCheckRequest;
import com.seoulcareconnect.dto.user.EmailSendRequest;
import com.seoulcareconnect.service.user.EmailVerifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/email")
public class EmailVerifyController {

    private final EmailVerifyService emailVerifyService;

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody EmailSendRequest request) {
        try {
            emailVerifyService.sendSignupCode(request.getEmail());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "인증번호가 발송되었습니다."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/check")
    public ResponseEntity<?> check(@RequestBody EmailCheckRequest request) {
        try {
            emailVerifyService.checkSignupCode(
                    request.getEmail(),
                    request.getVerifyCode()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "이메일 인증이 완료되었습니다."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}