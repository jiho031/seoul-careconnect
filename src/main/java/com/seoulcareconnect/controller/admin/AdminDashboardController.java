package com.seoulcareconnect.controller.admin;

import com.seoulcareconnect.service.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {

        model.addAttribute(
                "summary",
                adminDashboardService.getSummary()
        );

        model.addAttribute(
                "recentUsers",
                adminDashboardService.getRecentUsers()
        );

        model.addAttribute(
                "dailyStatuses",
                adminDashboardService.getRecentFiveDayStatus()
        );

        model.addAttribute(
                "notices",
                adminDashboardService.getRecentNotices()
        );

        return "admin/dashboard";
    }
}