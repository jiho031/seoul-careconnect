package com.seoulcareconnect.controller.admin;

import com.seoulcareconnect.dto.admin.AdminPolicyDTO;
import com.seoulcareconnect.entity.policy.PolicySource;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.entity.policy.enums.PolicyStatus;
import com.seoulcareconnect.repository.policy.PolicySourceRepository;
import com.seoulcareconnect.service.admin.AdminPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/policies")
public class AdminPolicyController {

    private static final List<String> SEOUL_DISTRICTS = List.of(
            "강남구", "강동구", "강북구", "강서구", "관악구",
            "광진구", "구로구", "금천구", "노원구", "도봉구",
            "동대문구", "동작구", "마포구", "서대문구", "서초구",
            "성동구", "성북구", "송파구", "양천구", "영등포구",
            "용산구", "은평구", "종로구", "중구", "중랑구"
    );

    private final AdminPolicyService adminPolicyService;
    private final PolicySourceRepository policySourceRepository;

    @GetMapping
    public String policies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PolicyCategory category,
            @RequestParam(required = false) PolicyStatus status,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) String district,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        int pageSize = 8;

        Page<AdminPolicyDTO> policyPage =
                adminPolicyService.getPolicies(
                        keyword,
                        category,
                        status,
                        sourceId,
                        district,
                        page,
                        pageSize
                );

        List<PolicySource> sources =
                policySourceRepository
                        .findAllByIsActiveTrueOrderBySourceNameAsc();

        model.addAttribute("policyPage", policyPage);
        model.addAttribute("policies", policyPage.getContent());

        model.addAttribute(
                "totalPolicyCount",
                adminPolicyService.getTotalPolicyCount()
        );

        model.addAttribute(
                "publishedCount",
                adminPolicyService.getPublishedCount()
        );

        model.addAttribute(
                "pendingReviewCount",
                adminPolicyService.getPendingReviewCount()
        );

        model.addAttribute(
                "hiddenOrExpiredCount",
                adminPolicyService.getHiddenOrExpiredCount()
        );

        model.addAttribute("categories", PolicyCategory.values());
        model.addAttribute("statuses", PolicyStatus.values());
        model.addAttribute("sources", sources);
        model.addAttribute("districts", SEOUL_DISTRICTS);

        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSourceId", sourceId);
        model.addAttribute("selectedDistrict", district);

        return "admin/policies";
    }

    @PostMapping("/approve")
    public String approve(
            @RequestParam Long policyId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminPolicyService.approvePolicy(policyId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "정책이 공개 승인되었습니다."
            );
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/policies";
    }

    @PostMapping("/hide")
    public String hide(
            @RequestParam Long policyId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminPolicyService.hidePolicy(policyId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "정책이 숨김 처리되었습니다."
            );
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/policies";
    }

    @PostMapping("/unhide")
    public String unhide(
            @RequestParam Long policyId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminPolicyService.unhidePolicy(policyId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "정책 숨김이 해제되었습니다."
            );
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/policies";
    }

    @PostMapping("/needs-update")
    public String markNeedsUpdate(
            @RequestParam Long policyId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminPolicyService.markNeedsUpdate(policyId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "정책이 수정 필요 상태로 변경되었습니다."
            );
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/policies";
    }

    @PostMapping("/reject")
    public String reject(
            @RequestParam Long policyId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminPolicyService.rejectPolicy(policyId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "정책이 반려 처리되었습니다."
            );
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/policies";
    }
}