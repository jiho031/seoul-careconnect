package com.seoulcareconnect.controller.ai;

import com.seoulcareconnect.entity.ai.AiPolicyExplanation.ReviewStatus;
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

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/ai-review")
public class AdminAiReviewController {

    private final AiPolicyExplanationService explanationService;

    @GetMapping
    public String page(
            @RequestParam(required = false) ReviewStatus status,
            Model model
    ) {
        model.addAttribute("items", explanationService.findRecent(status));
        model.addAttribute("selectedStatus", status);
        model.addAttribute("reviewStatuses", new ReviewStatus[]{
                ReviewStatus.DRAFT,
                ReviewStatus.APPROVED,
                ReviewStatus.REJECTED
        });
        model.addAttribute("draftCount", explanationService.count(ReviewStatus.DRAFT));
        model.addAttribute("approvedCount", explanationService.count(ReviewStatus.APPROVED));
        model.addAttribute("rejectedCount", explanationService.count(ReviewStatus.REJECTED));
        model.addAttribute("aiEnabled", explanationService.isEnabled());
        model.addAttribute("aiModel", explanationService.modelName());
        return "admin/ai-review";
    }

    @PostMapping("/generate")
    public String generate(
            @RequestParam Long policyId,
            RedirectAttributes redirectAttributes
    ) {
        return runAction(
                () -> explanationService.generate(policyId),
                "AI 설명 초안을 생성했습니다.",
                redirectAttributes
        );
    }

    @PostMapping("/{explanationId}/regenerate")
    public String regenerate(
            @PathVariable Long explanationId,
            RedirectAttributes redirectAttributes
    ) {
        return runAction(
                () -> explanationService.regenerate(explanationId),
                "AI 설명을 다시 생성했습니다.",
                redirectAttributes
        );
    }

    @PostMapping("/{explanationId}/approve")
    public String approve(
            @PathVariable Long explanationId,
            @RequestParam(required = false) String comment,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        return runAction(
                () -> explanationService.approve(
                        explanationId,
                        reviewer(authentication),
                        comment
                ),
                "승인했습니다. 사용자 정책 상세 화면에 반영됩니다.",
                redirectAttributes
        );
    }

    @PostMapping("/{explanationId}/reject")
    public String reject(
            @PathVariable Long explanationId,
            @RequestParam(required = false) String comment,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        return runAction(
                () -> explanationService.reject(
                        explanationId,
                        reviewer(authentication),
                        comment
                ),
                "AI 설명 초안을 반려했습니다.",
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
