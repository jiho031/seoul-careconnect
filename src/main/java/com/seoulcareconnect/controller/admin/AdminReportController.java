package com.seoulcareconnect.controller.admin;

import com.seoulcareconnect.dto.admin.AdminReportDTO;
import com.seoulcareconnect.service.admin.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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