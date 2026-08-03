package com.seoulcareconnect.controller.admin;

import com.seoulcareconnect.dto.admin.AdminPolicyDTO;
import com.seoulcareconnect.entity.policy.PolicySource;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.entity.policy.enums.PolicyStatus;
import com.seoulcareconnect.repository.policy.PolicySourceRepository;
import com.seoulcareconnect.service.admin.AdminPolicyService;
import com.seoulcareconnect.util.AdminCsvUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

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
                "recentReviewPolicies",
                adminPolicyService.getRecentReviewPolicies()
        );

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

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadPolicies(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            PolicyCategory category,

            @RequestParam(required = false)
            PolicyStatus status,

            @RequestParam(required = false)
            Long sourceId,

            @RequestParam(required = false)
            String district
    ) {
        List<AdminPolicyDTO> policies =
                adminPolicyService.getDownloadPolicies(
                        keyword,
                        category,
                        status,
                        sourceId,
                        district
                );

        List<String> csvHeaders =
                List.of(
                        "정책 DB ID",
                        "외부 정책 ID",
                        "정책명",
                        "기관",
                        "분야",
                        "지원 대상",
                        "지역",
                        "자치구",
                        "신청 시작일",
                        "신청 종료일",
                        "신청 기간",
                        "신청 상태",
                        "공개 상태",
                        "수집 출처",
                        "출처 유형",
                        "조회수"
                );

        List<List<String>> rows =
                policies.stream()
                        .map(policy ->
                                List.of(
                                        AdminCsvUtil.safe(
                                                policy.getPolicyId()
                                        ),
                                        AdminCsvUtil.safe(
                                                policy.getExternalId()
                                        ),
                                        AdminCsvUtil.safe(
                                                policy.getTitle()
                                        ),
                                        AdminCsvUtil.safe(
                                                policy.getOrganization()
                                        ),
                                        AdminCsvUtil.safe(
                                                policy.getCategoryLabel()
                                        ),
                                        AdminCsvUtil.safe(
                                                policy.getTarget()
                                        ),
                                        AdminCsvUtil.safe(
                                                policy.getRegion()
                                        ),
                                        AdminCsvUtil.safe(
                                                policy.getDistrict()
                                        ),
                                        AdminCsvUtil.safe(
                                                policy.getStartDate()
                                        ),
                                        AdminCsvUtil.safe(
                                                policy.getEndDate()
                                        ),
                                        AdminCsvUtil.safe(
                                                policy.getApplicationPeriodText()
                                        ),
                                        AdminCsvUtil.safe(
                                                policy.getApplyStatusLabel()
                                        ),
                                        AdminCsvUtil.safe(
                                                policy.getStatusLabel()
                                        ),
                                        AdminCsvUtil.safe(
                                                policy.getSourceName()
                                        ),
                                        AdminCsvUtil.safe(
                                                policy.getSourceTypeLabel()
                                        ),
                                        AdminCsvUtil.safe(
                                                policy.getViewCount()
                                        )
                                )
                        )
                        .toList();

        byte[] csvFile =
                AdminCsvUtil.createCsv(
                        csvHeaders,
                        rows
                );

        String today =
                LocalDate.now()
                        .format(
                                DateTimeFormatter.BASIC_ISO_DATE
                        );

        String fileName =
                "policy_list_"
                        + today
                        + ".csv";

        ContentDisposition disposition =
                ContentDisposition
                        .attachment()
                        .filename(
                                fileName,
                                StandardCharsets.UTF_8
                        )
                        .build();

        HttpHeaders responseHeaders =
                new HttpHeaders();

        responseHeaders.setContentType(
                new MediaType(
                        "text",
                        "csv",
                        StandardCharsets.UTF_8
                )
        );

        responseHeaders.setContentDisposition(
                disposition
        );

        responseHeaders.setContentLength(
                csvFile.length
        );

        return ResponseEntity
                .ok()
                .headers(responseHeaders)
                .body(csvFile);
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