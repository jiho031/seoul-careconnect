package com.seoulcareconnect.controller.admin;

import com.seoulcareconnect.dto.admin.AdminSyncLogDTO;
import com.seoulcareconnect.entity.admin.enums.AdminActivityType;
import com.seoulcareconnect.entity.policy.enums.SyncType;
import com.seoulcareconnect.service.admin.AdminActivityLogService;
import com.seoulcareconnect.service.admin.AdminCollectionService;
import com.seoulcareconnect.service.policy.PolicyCollectService;
import com.seoulcareconnect.service.policy.PolicyCollectionSummary;
import com.seoulcareconnect.util.AdminCsvUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminCollectionController {

    private final AdminCollectionService
            adminCollectionService;

    private final PolicyCollectService
            policyCollectService;

    private final AdminActivityLogService
            adminActivityLogService;

    @GetMapping("/admin/collection")
    public String collection(
            Model model
    ) {
        model.addAttribute(
                "summary",
                adminCollectionService.getSummary()
        );

        model.addAttribute(
                "sourceStatuses",
                adminCollectionService
                        .getSourceStatuses()
        );

        model.addAttribute(
                "syncLogs",
                adminCollectionService
                        .getRecentSyncLogs()
        );

        model.addAttribute(
                "availableSources",
                policyCollectService
                        .availableSourceNames()
        );

        return "admin/collection";
    }

    @GetMapping("/admin/collection/download")
    public ResponseEntity<byte[]>
    downloadCollectionLogs() {

        List<AdminSyncLogDTO> logs =
                adminCollectionService
                        .getDownloadSyncLogs();

        List<String> headers =
                List.of(
                        "로그 ID",
                        "API 출처",
                        "수집 유형",
                        "수집 상태",
                        "수집 시작 시간",
                        "수집 종료 시간",
                        "소요 시간",
                        "수집 성공",
                        "수집 실패",
                        "중복 제외",
                        "오류 메시지"
                );

        List<List<String>> rows =
                logs.stream()
                        .map(log ->
                                List.of(
                                        AdminCsvUtil.safe(
                                                log.getLogId()
                                        ),
                                        AdminCsvUtil.safe(
                                                log.getSourceName()
                                        ),
                                        AdminCsvUtil.safe(
                                                log.getSyncTypeLabel()
                                        ),
                                        AdminCsvUtil.safe(
                                                log.getStatusLabel()
                                        ),
                                        AdminCsvUtil.safe(
                                                log.getStartedAtText()
                                        ),
                                        AdminCsvUtil.safe(
                                                log.getEndedAtText()
                                        ),
                                        AdminCsvUtil.safe(
                                                log.getDurationText()
                                        ),
                                        AdminCsvUtil.safe(
                                                log.getSuccessCount()
                                        ),
                                        AdminCsvUtil.safe(
                                                log.getFailCount()
                                        ),
                                        AdminCsvUtil.safe(
                                                log.getDuplicateCount()
                                        ),
                                        AdminCsvUtil.safe(
                                                log.getSafeErrorMessage()
                                        )
                                )
                        )
                        .toList();

        byte[] csvFile =
                AdminCsvUtil.createCsv(
                        headers,
                        rows
                );

        String today =
                LocalDate.now()
                        .format(
                                DateTimeFormatter.BASIC_ISO_DATE
                        );

        String fileName =
                "api_collection_logs_"
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

        HttpHeaders headersResponse =
                new HttpHeaders();

        headersResponse.setContentType(
                new MediaType(
                        "text",
                        "csv",
                        StandardCharsets.UTF_8
                )
        );

        headersResponse.setContentDisposition(
                disposition
        );

        headersResponse.setContentLength(
                csvFile.length
        );

        return ResponseEntity
                .ok()
                .headers(headersResponse)
                .body(csvFile);
    }

    @PostMapping("/admin/collection/run")
    public String runCollection(
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        try {
            PolicyCollectionSummary result =
                    policyCollectService.collectAll(
                            SyncType.MANUAL
                    );

            String adminName =
                    authentication == null
                            ? "관리자"
                            : authentication.getName();

            adminActivityLogService.record(
                    null,
                    adminName,
                    AdminActivityType.COLLECTION_RUN,
                    null,
                    "전체 정책 API",
                    "수동 API 재수집을 실행했습니다."
            );

            redirectAttributes.addFlashAttribute(
                    "collectionSuccess",
                    "수동 재수집이 완료되었습니다."
            );

        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute(
                    "collectionError",
                    "수동 재수집 중 오류가 발생했습니다: "
                            + exception.getMessage()
            );
        }

        return "redirect:/admin/collection";
    }

    @PostMapping("/admin/collection/run-source")
    public String runSourceCollection(
            @RequestParam
            String sourceName,

            RedirectAttributes redirectAttributes
    ) {
        try {
            policyCollectService.collectOne(
                    sourceName,
                    SyncType.MANUAL
            );

            redirectAttributes.addFlashAttribute(
                    "collectionSuccess",
                    sourceName
                            + " 재수집이 완료되었습니다."
            );

        } catch (Exception exception) {
            log.error(
                    "{} 수동 수집 실패",
                    sourceName,
                    exception
            );

            redirectAttributes.addFlashAttribute(
                    "collectionError",
                    exception.getMessage() == null
                            ? sourceName
                            + " 재수집 중 오류가 발생했습니다."
                            : exception.getMessage()
            );
        }

        return "redirect:/admin/collection";
    }
}