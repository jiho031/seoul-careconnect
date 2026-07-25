package com.seoulcareconnect.controller.ai;

import com.seoulcareconnect.service.ai.AiAssistantUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = AiAssistantController.class)
public class AiAssistantExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation() {
        return ResponseEntity.badRequest().body(Map.of(
                "message",
                "질문은 1자 이상 500자 이하로 입력해 주세요."
        ));
    }

    @ExceptionHandler(AiAssistantUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleUnavailable(
            AiAssistantUnavailableException exception
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "message",
                exception.getMessage()
        ));
    }
}
