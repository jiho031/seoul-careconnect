package com.seoulcareconnect.controller.ai;

import com.seoulcareconnect.service.ai.AiPolicyExplanationService;
import lombok.RequiredArgsConstructor;
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

    private final AiPolicyExplanationService explanationService;

    @GetMapping
    public String page(Model model) {
        model.addAttribute("items", explanationService.findRecentGenerated());
        model.addAttribute("generatedCount", explanationService.countGenerated());
        model.addAttribute(
                "todayGeneratedCount",
                explanationService.countGeneratedSince(
                        LocalDate.now(ZoneId.of("Asia/Seoul")).atStartOfDay()
                )
        );
        model.addAttribute("openAiConfigured", explanationService.isOpenAiConfigured());
        model.addAttribute("aiModel", explanationService.modelName());
        return "admin/ai-review";
    }

    @PostMapping("/{explanationId}/edit")
    public String edit(
            @PathVariable Long explanationId,
            @RequestParam String easySummary,
            @RequestParam String eligibilitySummary,
            @RequestParam String benefitSummary,
            @RequestParam String applicationSummary,
            @RequestParam String cautionSummary,
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
                redirectAttributes
        );
    }

    @PostMapping("/{explanationId}/delete")
    public String delete(
            @PathVariable Long explanationId,
            RedirectAttributes redirectAttributes
    ) {
        return runAction(
                () -> explanationService.delete(explanationId),
                "AI 정책 요약을 삭제했습니다. 다음 도우미 실행 때 다시 생성됩니다.",
                redirectAttributes
        );
    }

    private String runAction(
            Runnable action,
            String successMessage,
            RedirectAttributes redirectAttributes
    ) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/ai-review";
    }

    private String reviewer(Authentication authentication) {
        return authentication == null ? "admin" : authentication.getName();
    }
}
