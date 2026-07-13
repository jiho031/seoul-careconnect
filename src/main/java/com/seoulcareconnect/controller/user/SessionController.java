package com.seoulcareconnect.controller.user;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/session")
public class SessionController {

    @PostMapping("/extend")
    public ResponseEntity<Map<String, Object>> extendSession(
            HttpSession session
    ) {
        /*
         * 이 요청이 정상 처리되면 HttpSession의 마지막 접근 시간이
         * 자동으로 갱신되어 세션이 다시 30분 동안 유지된다.
         */
        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "expiresIn", session.getMaxInactiveInterval()
                )
        );
    }
}