package com.seoulcareconnect.controller.admin;

import com.seoulcareconnect.entity.policy.enums.SyncType;
import com.seoulcareconnect.service.admin.AdminCollectionService;
import com.seoulcareconnect.service.policy.PolicyCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminCollectionController {

    private final AdminCollectionService adminCollectionService;
    private final PolicyCollectService policyCollectService;

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
    public String runManualCollection(
            RedirectAttributes redirectAttributes
    ) {
        System.out.println("######## 수동 수집 컨트롤러 진입 ########");
        try {
            log.info("===== 관리자 수동 수집 요청 시작 =====");

            policyCollectService.collectAll(SyncType.MANUAL);

            log.info("===== 관리자 수동 수집 서비스 실행 완료 =====");

            redirectAttributes.addFlashAttribute(
                    "collectionSuccess",
                    "수동 재수집이 완료되었습니다."
            );

        } catch (Exception e) {
            log.error("===== 관리자 수동 수집 실패 =====", e);

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