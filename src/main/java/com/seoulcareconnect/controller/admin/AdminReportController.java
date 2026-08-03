package com.seoulcareconnect.controller.admin;

import com.seoulcareconnect.dto.admin.AdminReportDTO;
import com.seoulcareconnect.service.admin.AdminReportService;
import com.seoulcareconnect.util.AdminCsvUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;


@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/reports")
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping
    public String reports(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        int pageSize = 8;

        Page<AdminReportDTO> reportPage =
                adminReportService.getReports(
                        keyword,
                        reportType,
                        status,
                        page,
                        pageSize
                );

        model.addAttribute("reportPage", reportPage);
        model.addAttribute("reports", reportPage.getContent());

        model.addAttribute(
                "totalCount",
                adminReportService.getTotalCount()
        );

        model.addAttribute(
                "receivedCount",
                adminReportService.getReceivedCount()
        );

        model.addAttribute(
                "inReviewCount",
                adminReportService.getInReviewCount()
        );

        model.addAttribute(
                "completedCount",
                adminReportService.getCompletedCount()
        );

        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedReportType", reportType);
        model.addAttribute("selectedStatus", status);

        return "admin/reports";
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadReports(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            String reportType,

            @RequestParam(required = false)
            String status
    ) {
        List<AdminReportDTO> reports =
                adminReportService.getDownloadReports(
                        keyword,
                        reportType,
                        status
                );

        List<String> csvHeaders =
                List.of(
                        "신고 ID",
                        "회원 ID",
                        "정책 ID",
                        "신고자 이름",
                        "신고자 이메일",
                        "신고 제목",
                        "신고 대상 정책",
                        "신고 유형",
                        "신고 내용",
                        "출처 URL",
                        "처리 상태",
                        "관리자 메모",
                        "접수 시간",
                        "처리 시간"
                );

        List<List<String>> rows =
                reports.stream()
                        .map(report ->
                                List.of(
                                        AdminCsvUtil.safe(
                                                report.getReportId()
                                        ),
                                        AdminCsvUtil.safe(
                                                report.getUserId()
                                        ),
                                        AdminCsvUtil.safe(
                                                report.getPolicyId()
                                        ),
                                        AdminCsvUtil.safe(
                                                report.getReporterName()
                                        ),
                                        AdminCsvUtil.safe(
                                                report.getReporterEmail()
                                        ),
                                        AdminCsvUtil.safe(
                                                report.getTitle()
                                        ),
                                        AdminCsvUtil.safe(
                                                report.getPolicyTitle()
                                        ),
                                        AdminCsvUtil.safe(
                                                report.getReportTypeLabel()
                                        ),
                                        AdminCsvUtil.safe(
                                                report.getContent()
                                        ),
                                        AdminCsvUtil.safe(
                                                report.getSourceUrl()
                                        ),
                                        AdminCsvUtil.safe(
                                                report.getStatusLabel()
                                        ),
                                        AdminCsvUtil.safe(
                                                report.getAdminMemo()
                                        ),
                                        AdminCsvUtil.safe(
                                                report.getCreatedAtText()
                                        ),
                                        AdminCsvUtil.safe(
                                                report.getProcessedAtText()
                                        )
                                )
                        )
                        .toList();

        byte[] csvFile =
                AdminCsvUtil.createCsv(
                        csvHeaders,
                        rows
                );

        String today =
                LocalDate.now()
                        .format(
                                DateTimeFormatter.BASIC_ISO_DATE
                        );

        String fileName =
                "user_report_logs_"
                        + today
                        + ".csv";

        ContentDisposition disposition =
                ContentDisposition
                        .attachment()
                        .filename(
                                fileName,
                                StandardCharsets.UTF_8
                        )
                        .build();

        HttpHeaders responseHeaders =
                new HttpHeaders();

        responseHeaders.setContentType(
                new MediaType(
                        "text",
                        "csv",
                        StandardCharsets.UTF_8
                )
        );

        responseHeaders.setContentDisposition(
                disposition
        );

        responseHeaders.setContentLength(
                csvFile.length
        );

        return ResponseEntity
                .ok()
                .headers(responseHeaders)
                .body(csvFile);
    }

    @PostMapping("/status")
    public String changeStatus(
            @RequestParam Long reportId,
            @RequestParam String status,
            @RequestParam(required = false) String adminMemo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminReportService.changeStatus(
                    reportId,
                    status,
                    adminMemo
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "사용자 신고 상태가 변경되었습니다."
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/reports";
    }
}