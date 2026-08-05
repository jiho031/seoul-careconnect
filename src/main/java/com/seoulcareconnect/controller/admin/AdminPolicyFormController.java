package com.seoulcareconnect.controller.admin;

import com.seoulcareconnect.dto.admin.AdminPolicyFormDTO;
import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.service.admin.AdminPolicyFormService;
import com.seoulcareconnect.service.ai.AiModelGateway;
import com.seoulcareconnect.service.ai.AiPolicyExplanationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.seoulcareconnect.dto.ai.AiPolicyExplanationDto;
import com.seoulcareconnect.service.ai.AiPolicyExplanationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AdminPolicyFormController {

    private final AdminPolicyFormService
            adminPolicyFormService;

    private final AiPolicyExplanationService
            aiPolicyExplanationService;

    @GetMapping("/admin/policies/new")
    public String createForm(
            @RequestParam(required = false)
            Long reportId,

            Model model
    ) {
        AdminPolicyFormDTO policyForm;

        if (reportId != null) {
            policyForm =
                    adminPolicyFormService
                            .createFormFromReport(reportId);
        } else {
            policyForm =
                    adminPolicyFormService
                            .createEmptyForm();
        }

        model.addAttribute(
                "policyForm",
                policyForm
        );

        model.addAttribute(
                "reportId",
                reportId
        );

        addFormOptions(model);

        model.addAttribute(
                "editMode",
                false
        );

        return "admin/policy-form";
    }

    /**
     * 기존 정책 수정 화면
     */
    @GetMapping("/admin/policies/{policyId}/edit")
    public String editForm(
            @PathVariable
            Long policyId,

            Model model
    ) {
        model.addAttribute(
                "policyForm",
                adminPolicyFormService
                        .getPolicyForm(policyId)
        );

        addFormOptions(model);

        model.addAttribute(
                "editMode",
                true
        );

        return "admin/policy-form";
    }

    /**
     * 신규 등록 및 수정 저장
     */
    @PostMapping("/admin/policies/save")
    public String savePolicy(
            @ModelAttribute("policyForm")
            AdminPolicyFormDTO policyForm,

            @RequestParam(
                    name = "saveAction",
                    defaultValue = "REVIEW"
            )
            String saveAction,

            RedirectAttributes redirectAttributes
    ) {
        Long policyId =
                adminPolicyFormService.savePolicy(
                        policyForm,
                        saveAction
                );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                resolveSuccessMessage(saveAction)
        );

        return "redirect:/admin/policies/"
                + policyId
                + "/edit";
    }

    private void addFormOptions(
            Model model
    ) {
        model.addAttribute(
                "categories",
                PolicyCategory.values()
        );

        model.addAttribute(
                "applyStatuses",
                ApplyStatus.values()
        );
    }

    private String resolveSuccessMessage(
            String saveAction
    ) {
        return switch (saveAction) {
            case "DRAFT" ->
                    "정책이 임시 저장되었습니다.";

            case "APPROVE" ->
                    "정책이 승인 및 공개 상태로 저장되었습니다.";

            default ->
                    "정책이 검수 대기 상태로 저장되었습니다.";
        };
    }

    /**
     * 정책 수정 화면 AI 쉬운 설명 생성
     */
    @PostMapping("/admin/policies/{policyId}/ai-summary")
    @ResponseBody
    public ResponseEntity<?> generateAiSummary(
            @PathVariable Long policyId
    ) {
        try {
            AiPolicyExplanationDto explanation =
                    aiPolicyExplanationService.generateIfMissing(policyId);

            String checkPoint =
                    buildCheckPoint(explanation);

            return ResponseEntity.ok(
                    Map.of(
                            "easySummary",
                            safeValue(
                                    explanation.easySummary()
                            ),
                            "checkPoint",
                            checkPoint
                    )
            );

        } catch (IllegalArgumentException exception) {
            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    exception.getMessage()
                            )
                    );

        } catch (AiModelGateway.UnavailableException exception) {
            return ResponseEntity
                    .status(
                            HttpStatus.SERVICE_UNAVAILABLE
                    )
                    .body(
                            Map.of(
                                    "message",
                                    exception.getMessage()
                            )
                    );

        } catch (RuntimeException exception) {
            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            Map.of(
                                    "message",
                                    "AI 요약 생성 중 오류가 발생했습니다."
                            )
                    );
        }
    }

    private String buildCheckPoint(
            AiPolicyExplanationDto explanation
    ) {
        StringBuilder builder =
                new StringBuilder();

        appendSection(
                builder,
                "신청 자격",
                explanation.eligibilitySummary()
        );

        appendSection(
                builder,
                "신청 방법",
                explanation.applicationSummary()
        );

        appendSection(
                builder,
                "확인사항",
                explanation.cautionSummary()
        );

        return builder.toString().trim();
    }

    private void appendSection(
            StringBuilder builder,
            String title,
            String content
    ) {
        if (content == null
                || content.isBlank()) {
            return;
        }

        if (!builder.isEmpty()) {
            builder.append("\n\n");
        }

        builder.append("[")
                .append(title)
                .append("]\n")
                .append(content.trim());
    }

    private String safeValue(
            String value
    ) {
        return value == null
                ? ""
                : value;
    }
}