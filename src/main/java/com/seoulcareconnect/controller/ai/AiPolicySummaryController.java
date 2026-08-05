package com.seoulcareconnect.controller.ai;

import com.seoulcareconnect.dto.ai.AiPolicySummaryStatusResponse;
import com.seoulcareconnect.service.ai.AiPolicySummaryGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(
        value = "/api/ai/policies",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
public class AiPolicySummaryController {

    private final AiPolicySummaryGenerationService summaryGenerationService;

    @PostMapping("/{policyId:\\d+}/summary")
    public ResponseEntity<AiPolicySummaryStatusResponse> start(
            @PathVariable Long policyId
    ) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(summaryGenerationService.start(policyId));
    }

    @GetMapping("/{policyId:\\d+}/summary/status")
    public AiPolicySummaryStatusResponse status(
            @PathVariable Long policyId
    ) {
        return summaryGenerationService.status(policyId);
    }

    @ExceptionHandler(AiPolicySummaryGenerationService.SummaryUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleUnavailable(
            AiPolicySummaryGenerationService.SummaryUnavailableException exception
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "message",
                exception.getMessage()
        ));
    }
}
