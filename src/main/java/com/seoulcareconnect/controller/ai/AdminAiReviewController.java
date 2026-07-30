package com.seoulcareconnect.controller.ai;

import com.seoulcareconnect.dto.ai.AiPolicyExplanationDto;
import com.seoulcareconnect.service.ai.AiPolicyExplanationService;
import com.seoulcareconnect.service.ai.AiSummaryAutomationService;
import com.seoulcareconnect.service.ai.AiSummarySettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.ZoneId;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/ai-review")
public class AdminAiReviewController {

    private static final int PAGE_SIZE = 10;

    private final AiPolicyExplanationService explanationService;
    private final AiSummaryAutomationService automationService;
    private final AiSummarySettingService settingService;

    @GetMapping
    public String page(
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        int safePage = Math.max(page, 0);
        Page<AiPolicyExplanationDto> itemPage =
                explanationService.findGeneratedPage(safePage, PAGE_SIZE);

        // 삭제 등으로 현재 페이지가 사라졌다면 마지막 유효 페이지로 이동한다.
        if (itemPage.getTotalPages() > 0 && safePage >= itemPage.getTotalPages()) {
            return "redirect:/admin/ai-review?page=" + (itemPage.getTotalPages() - 1);
        }

        int paginationStart = 0;
        int paginationEnd = 0;
        if (itemPage.getTotalPages() > 0) {
            paginationStart = Math.max(0, itemPage.getNumber() - 2);
            paginationEnd = Math.min(itemPage.getTotalPages() - 1, paginationStart + 4);
            paginationStart = Math.max(0, paginationEnd - 4);
        }

        model.addAttribute("items", itemPage.getContent());
        model.addAttribute("itemPage", itemPage);
        model.addAttribute("paginationStart", paginationStart);
        model.addAttribute("paginationEnd", paginationEnd);
        model.addAttribute("generatedCount", explanationService.countGenerated());
        model.addAttribute(
                "todayGeneratedCount",
                explanationService.countGeneratedSince(
                        LocalDate.now(ZoneId.of("Asia/Seoul")).atStartOfDay()
                )
        );
        model.addAttribute("openAiConfigured", explanationService.isOpenAiConfigured());
        model.addAttribute("aiModel", explanationService.modelName());
        model.addAttribute("automaticSummaryEnabled", settingService.isAutomaticSummaryEnabled());
        model.addAttribute("missingSummaryCount", automationService.countMissingSummaries());
        model.addAttribute("manualGenerationStatus", automationService.manualGenerationStatus());
        return "admin/ai-review";
    }

    @PostMapping("/automatic")
    public String updateAutomaticSummary(
            @RequestParam(defaultValue = "false") boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            RedirectAttributes redirectAttributes
    ) {
        return runAction(
                () -> {
                    if (enabled && !explanationService.isOpenAiConfigured()) {
                        throw new IllegalStateException(
                                "AI 기능을 활성화하고 OPENAI_API_KEY를 설정한 뒤 자동 요약을 켜 주세요."
                        );
                    }
                    settingService.updateAutomaticSummaryEnabled(enabled);
                },
                enabled
                        ? "자동 AI 요약을 켰습니다. 새로 등록되는 정책부터 자동으로 요약합니다."
                        : "자동 AI 요약을 껐습니다. 기존 요약은 유지되며 수동 전체 요약을 사용할 수 있습니다.",
                page,
                redirectAttributes
        );
    }

    @PostMapping("/generate-missing")
    public String generateMissing(
            @RequestParam(defaultValue = "0") int page,
            RedirectAttributes redirectAttributes
    ) {
        try {
            AiSummaryAutomationService.StartResult result =
                    automationService.startManualGeneration();
            if (result.started()) {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "미요약 정책 " + result.requestedCount()
                                + "건의 전체 요약을 시작했습니다."
                );
            } else {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "요약이 필요한 정책이 없습니다."
                );
            }
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        redirectAttributes.addAttribute("page", Math.max(page, 0));
        return "redirect:/admin/ai-review";
    }

    @PostMapping("/{explanationId}/edit")
    public String edit(
            @PathVariable Long explanationId,
            @RequestParam String easySummary,
            @RequestParam String eligibilitySummary,
            @RequestParam String benefitSummary,
            @RequestParam String applicationSummary,
            @RequestParam String cautionSummary,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        return runAction(
                () -> explanationService.update(
                        explanationId,
                        easySummary,
                        eligibilitySummary,
                        benefitSummary,
                        applicationSummary,
                        cautionSummary,
                        reviewer(authentication)
                ),
                "AI 정책 요약을 수정했습니다.",
                page,
                redirectAttributes
        );
    }

    @PostMapping("/{explanationId}/delete")
    public String delete(
            @PathVariable Long explanationId,
            @RequestParam(defaultValue = "0") int page,
            RedirectAttributes redirectAttributes
    ) {
        return runAction(
                () -> explanationService.delete(explanationId),
                "AI 정책 요약을 삭제했습니다. 자동 요약 또는 수동 전체 요약으로 다시 생성할 수 있습니다.",
                page,
                redirectAttributes
        );
    }

    private String runAction(
            Runnable action,
            String successMessage,
            int page,
            RedirectAttributes redirectAttributes
    ) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        redirectAttributes.addAttribute("page", Math.max(page, 0));
        return "redirect:/admin/ai-review";
    }

    private String reviewer(Authentication authentication) {
        return authentication == null ? "admin" : authentication.getName();
    }
}
