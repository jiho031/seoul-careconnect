package com.seoulcareconnect.controller.report;

import com.seoulcareconnect.dto.report.MissingPolicyReportCardDto;
import com.seoulcareconnect.service.report.MissingPolicyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/reports")
public class MissingPolicyReportApiController {

    private final MissingPolicyReportService missingPolicyReportService;

    // 마이페이지에서 "내가 접수한 신고 내역" 조회용
    @GetMapping
    public ResponseEntity<List<MissingPolicyReportCardDto>> getMyReports(@PathVariable Long userId) {
        return ResponseEntity.ok(missingPolicyReportService.getMyReports(userId));
    }
}
