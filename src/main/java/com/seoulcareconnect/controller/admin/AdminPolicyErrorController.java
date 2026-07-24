package com.seoulcareconnect.controller.admin;

import com.seoulcareconnect.dto.admin.AdminPolicyErrorDTO;
import com.seoulcareconnect.entity.policy.PolicySource;
import com.seoulcareconnect.entity.policy.enums.PolicyErrorStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyErrorType;
import com.seoulcareconnect.repository.policy.PolicySourceRepository;
import com.seoulcareconnect.service.admin.AdminPolicyErrorService;
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
@RequestMapping("/admin/policy-errors")
public class AdminPolicyErrorController {

    private final AdminPolicyErrorService adminPolicyErrorService;
    private final PolicySourceRepository policySourceRepository;

    @GetMapping
    public String policyErrors(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PolicyErrorType errorType,
            @RequestParam(required = false) PolicyErrorStatus status,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        int pageSize = 8;

        Page<AdminPolicyErrorDTO> errorPage =
                adminPolicyErrorService.getPolicyErrors(
                        keyword,
                        errorType,
                        status,
                        sourceId,
                        page,
                        pageSize
                );

        List<PolicySource> sources =
                policySourceRepository
                        .findAllByIsActiveTrueOrderBySourceNameAsc();

        model.addAttribute("errorPage", errorPage);
        model.addAttribute("errors", errorPage.getContent());
        model.addAttribute("sources", sources);

        model.addAttribute(
                "totalErrorCount",
                adminPolicyErrorService.getTotalErrorCount()
        );

        model.addAttribute(
                "missingValueCount",
                adminPolicyErrorService.getMissingValueCount()
        );

        model.addAttribute(
                "dateErrorCount",
                adminPolicyErrorService.getDateErrorCount()
        );

        model.addAttribute(
                "urlErrorCount",
                adminPolicyErrorService.getUrlErrorCount()
        );

        model.addAttribute("errorTypes", PolicyErrorType.values());
        model.addAttribute("errorStatuses", PolicyErrorStatus.values());

        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedErrorType", errorType);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSourceId", sourceId);

        return "admin/policy-errors";
    }

    @PostMapping("/status")
    public String changeStatus(
            @RequestParam Long errorId,
            @RequestParam PolicyErrorStatus status,
            @RequestParam(required = false) String adminMemo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminPolicyErrorService.changeStatus(
                    errorId,
                    status,
                    adminMemo
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "정책 오류 상태가 변경되었습니다."
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/policy-errors";
    }
}