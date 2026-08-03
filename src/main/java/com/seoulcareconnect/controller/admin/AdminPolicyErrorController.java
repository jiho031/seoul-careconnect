package com.seoulcareconnect.controller.admin;

import com.seoulcareconnect.dto.admin.AdminPolicyErrorDTO;
import com.seoulcareconnect.entity.policy.PolicySource;
import com.seoulcareconnect.entity.policy.enums.PolicyErrorStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyErrorType;
import com.seoulcareconnect.repository.policy.PolicySourceRepository;
import com.seoulcareconnect.service.admin.AdminPolicyErrorService;
import com.seoulcareconnect.util.AdminCsvUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/admin/policy-errors")
public class AdminPolicyErrorController {

    private final AdminPolicyErrorService
            adminPolicyErrorService;

    private final PolicySourceRepository
            policySourceRepository;

    @GetMapping
    public String policyErrors(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            PolicyErrorType errorType,

            @RequestParam(required = false)
            PolicyErrorStatus status,

            @RequestParam(required = false)
            Long sourceId,

            @RequestParam(defaultValue = "0")
            int page,

            Model model
    ) {
        int pageSize = 8;

        Page<AdminPolicyErrorDTO> errorPage =
                adminPolicyErrorService
                        .getPolicyErrors(
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

        model.addAttribute(
                "errorPage",
                errorPage
        );

        model.addAttribute(
                "errors",
                errorPage.getContent()
        );

        model.addAttribute(
                "sources",
                sources
        );

        model.addAttribute(
                "totalErrorCount",
                adminPolicyErrorService
                        .getTotalErrorCount()
        );

        model.addAttribute(
                "missingValueCount",
                adminPolicyErrorService
                        .getMissingValueCount()
        );

        model.addAttribute(
                "dateErrorCount",
                adminPolicyErrorService
                        .getDateErrorCount()
        );

        model.addAttribute(
                "urlErrorCount",
                adminPolicyErrorService
                        .getUrlErrorCount()
        );

        model.addAttribute(
                "errorTypes",
                PolicyErrorType.values()
        );

        model.addAttribute(
                "errorStatuses",
                PolicyErrorStatus.values()
        );

        model.addAttribute(
                "keyword",
                keyword
        );

        model.addAttribute(
                "selectedErrorType",
                errorType
        );

        model.addAttribute(
                "selectedStatus",
                status
        );

        model.addAttribute(
                "selectedSourceId",
                sourceId
        );

        return "admin/policy-errors";
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadPolicyErrors(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            PolicyErrorType errorType,

            @RequestParam(required = false)
            PolicyErrorStatus status,

            @RequestParam(required = false)
            Long sourceId
    ) {
        List<AdminPolicyErrorDTO> errors =
                adminPolicyErrorService
                        .getDownloadPolicyErrors(
                                keyword,
                                errorType,
                                status,
                                sourceId
                        );

        List<String> csvHeaders =
                List.of(
                        "오류 ID",
                        "정책 DB ID",
                        "외부 정책 ID",
                        "정책명",
                        "API 출처",
                        "오류 유형",
                        "오류 내용",
                        "처리 상태",
                        "관리자 메모",
                        "접수 시간",
                        "처리 시간",
                        "지원 대상",
                        "지역",
                        "자치구",
                        "신청 시작일",
                        "신청 종료일",
                        "공식 URL"
                );

        List<List<String>> rows =
                errors.stream()
                        .map(error ->
                                List.of(
                                        AdminCsvUtil.safe(
                                                error.getErrorId()
                                        ),
                                        AdminCsvUtil.safe(
                                                error.getPolicyId()
                                        ),
                                        AdminCsvUtil.safe(
                                                error.getExternalId()
                                        ),
                                        AdminCsvUtil.safe(
                                                error.getPolicyTitle()
                                        ),
                                        AdminCsvUtil.safe(
                                                error.getSourceName()
                                        ),
                                        AdminCsvUtil.safe(
                                                error.getErrorTypeLabel()
                                        ),
                                        AdminCsvUtil.safe(
                                                error.getErrorMessage()
                                        ),
                                        AdminCsvUtil.safe(
                                                error.getStatusLabel()
                                        ),
                                        AdminCsvUtil.safe(
                                                error.getAdminMemo()
                                        ),
                                        AdminCsvUtil.safe(
                                                error.getCreatedAtText()
                                        ),
                                        AdminCsvUtil.safe(
                                                error.getProcessedAtText()
                                        ),
                                        AdminCsvUtil.safe(
                                                error.getTarget()
                                        ),
                                        AdminCsvUtil.safe(
                                                error.getRegion()
                                        ),
                                        AdminCsvUtil.safe(
                                                error.getDistrict()
                                        ),
                                        AdminCsvUtil.safe(
                                                error.getStartDateText()
                                        ),
                                        AdminCsvUtil.safe(
                                                error.getEndDateText()
                                        ),
                                        AdminCsvUtil.safe(
                                                error.getOfficialUrl()
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
                "policy_error_logs_"
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

    @PostMapping("/status")
    public String changeStatus(
            @RequestParam
            Long errorId,

            @RequestParam
            PolicyErrorStatus status,

            @RequestParam(required = false)
            String adminMemo,

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

        } catch (
                IllegalArgumentException |
                IllegalStateException exception
        ) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return "redirect:/admin/policy-errors";
    }

    /**
     * 모달에서 정책 직접 수정
     */
    @PostMapping("/quick-edit")
    public String quickEdit(
            @RequestParam
            Long errorId,

            @RequestParam(required = false)
            String title,

            @RequestParam(required = false)
            String target,

            @RequestParam(required = false)
            String region,

            @RequestParam(required = false)
            String district,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false)
            String officialUrl,

            @RequestParam(required = false)
            String benefit,

            @RequestParam(required = false)
            String selectionCriteria,

            @RequestParam(required = false)
            String requiredDocumentsText,

            @RequestParam(required = false)
            String contentText,

            @RequestParam(required = false)
            String adminMemo,

            RedirectAttributes redirectAttributes
    ) {
        try {
            adminPolicyErrorService.quickEdit(
                    errorId,
                    title,
                    target,
                    region,
                    district,
                    startDate,
                    endDate,
                    officialUrl,
                    benefit,
                    selectionCriteria,
                    requiredDocumentsText,
                    contentText,
                    adminMemo
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "정책 정보가 수정되었으며 검수 대기 상태로 변경되었습니다."
            );

        } catch (
                IllegalArgumentException |
                IllegalStateException exception
        ) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return "redirect:/admin/policy-errors";
    }
}