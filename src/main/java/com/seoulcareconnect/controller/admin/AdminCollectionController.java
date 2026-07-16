package com.seoulcareconnect.controller.admin;

import com.seoulcareconnect.entity.admin.enums.AdminActivityType;
import com.seoulcareconnect.entity.policy.enums.SyncType;
import com.seoulcareconnect.service.admin.AdminActivityLogService;
import com.seoulcareconnect.service.admin.AdminCollectionService;
import com.seoulcareconnect.service.policy.PolicyCollectService;
import com.seoulcareconnect.service.policy.PolicyCollectionSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;




@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminCollectionController {

    private final AdminCollectionService adminCollectionService;
    private final PolicyCollectService policyCollectService;
    private final AdminActivityLogService adminActivityLogService;

    @GetMapping("/admin/collection")
    public String collection(Model model) {

        model.addAttribute(
                "summary",
                adminCollectionService.getSummary()
        );

        model.addAttribute(
                "sourceStatuses",
                adminCollectionService.getSourceStatuses()
        );

        model.addAttribute(
                "syncLogs",
                adminCollectionService.getRecentSyncLogs()
        );

        return "admin/collection";
    }

    @PostMapping("/admin/collection/run")
    public String runCollection(
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        try {
            PolicyCollectionSummary result =
                    policyCollectService.collectAll(
                            SyncType.MANUAL
                    );

            String adminName =
                    authentication == null
                            ? "관리자"
                            : authentication.getName();

            adminActivityLogService.record(
                    null,
                    adminName,
                    AdminActivityType.COLLECTION_RUN,
                    null,
                    "전체 정책 API",
                    "수동 API 재수집을 실행했습니다."
            );

            redirectAttributes.addFlashAttribute(
                    "collectionSuccess",
                    "수동 재수집이 완료되었습니다."
            );

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "collectionError",
                    "수동 재수집 중 오류가 발생했습니다: "
                            + e.getMessage()
            );
        }

        return "redirect:/admin/collection";
    }

}