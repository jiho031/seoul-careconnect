package com.seoulcareconnect.controller.admin;

import com.seoulcareconnect.entity.policy.enums.SyncType;
import com.seoulcareconnect.service.admin.AdminCollectionService;
import com.seoulcareconnect.service.policy.PolicyCollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminCollectionController {

    private final AdminCollectionService adminCollectionService;
    private final PolicyCollectService policyCollectService;

    /**
     * API 수집 현황 페이지
     */
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

    /**
     * 관리자 수동 전체 수집
     */
    @PostMapping("/admin/collection/run")
    public String runManualCollection(
            RedirectAttributes redirectAttributes
    ) {
        try {
            policyCollectService.collectAll(SyncType.MANUAL);

            redirectAttributes.addFlashAttribute(
                    "collectionSuccess",
                    "수동 재수집이 완료되었습니다."
            );
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "collectionError",
                    e.getMessage() == null
                            ? "수동 재수집 중 오류가 발생했습니다."
                            : e.getMessage()
            );
        }

        return "redirect:/admin/collection";
    }
}