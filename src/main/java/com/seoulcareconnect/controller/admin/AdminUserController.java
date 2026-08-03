package com.seoulcareconnect.controller.admin;

import com.seoulcareconnect.dto.admin.AdminUserDTO;
import com.seoulcareconnect.service.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.seoulcareconnect.util.AdminCsvUtil;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public String users(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            String role,

            @RequestParam(required = false)
            Boolean active,

            @RequestParam(required = false)
            String ageGroup,

            @RequestParam(defaultValue = "0")
            int page,

            Model model
    ) {
        int pageSize = 8;

        Page<AdminUserDTO> userPage =
                adminUserService.getUsers(
                        keyword,
                        role,
                        active,
                        ageGroup,
                        page,
                        pageSize
                );

        model.addAttribute(
                "userPage",
                userPage
        );

        model.addAttribute(
                "users",
                userPage.getContent()
        );

        model.addAttribute(
                "totalUserCount",
                adminUserService.getTotalUserCount()
        );

        model.addAttribute(
                "todayJoinedCount",
                adminUserService.getTodayJoinedCount()
        );

        model.addAttribute(
                "adminCount",
                adminUserService.getAdminCount()
        );

        model.addAttribute(
                "inactiveUserCount",
                adminUserService.getInactiveUserCount()
        );

        model.addAttribute(
                "keyword",
                keyword
        );

        model.addAttribute(
                "selectedRole",
                role
        );

        model.addAttribute(
                "selectedActive",
                active
        );

        model.addAttribute(
                "selectedAgeGroup",
                ageGroup
        );

        return "admin/users";
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadUsers(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            String role,

            @RequestParam(required = false)
            Boolean active,

            @RequestParam(required = false)
            String ageGroup
    ) {
        List<AdminUserDTO> users =
                adminUserService.getDownloadUsers(
                        keyword,
                        role,
                        active,
                        ageGroup
                );

        List<String> csvHeaders =
                List.of(
                        "회원 ID",
                        "이름",
                        "이메일",
                        "전화번호",
                        "가입 방식",
                        "회원 권한",
                        "연령대",
                        "지역",
                        "자치구",
                        "회원 상태",
                        "가입일"
                );

        List<List<String>> rows =
                users.stream()
                        .map(user ->
                                List.of(
                                        AdminCsvUtil.safe(
                                                user.getUserId()
                                        ),
                                        AdminCsvUtil.safe(
                                                user.getName()
                                        ),
                                        AdminCsvUtil.safe(
                                                user.getEmail()
                                        ),
                                        AdminCsvUtil.safe(
                                                user.getPhone()
                                        ),
                                        resolveProviderLabel(
                                                user.getProvider()
                                        ),
                                        AdminCsvUtil.safe(
                                                user.getRoleLabel()
                                        ),
                                        AdminCsvUtil.safe(
                                                user.getAgeGroupLabel()
                                        ),
                                        AdminCsvUtil.safe(
                                                user.getRegion()
                                        ),
                                        AdminCsvUtil.safe(
                                                user.getDistrict()
                                        ),
                                        AdminCsvUtil.safe(
                                                user.getStatusLabel()
                                        ),
                                        AdminCsvUtil.safe(
                                                user.getCreatedAtText()
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
                "user_list_"
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

    private String resolveProviderLabel(
            String provider
    ) {
        if (provider == null
                || provider.isBlank()
                || "LOCAL".equalsIgnoreCase(provider)) {

            return "일반 가입";
        }

        return switch (provider.toUpperCase()) {
            case "GOOGLE" -> "Google";
            case "KAKAO" -> "Kakao";
            case "NAVER" -> "Naver";
            default -> provider;
        };
    }

    @PostMapping("/deactivate")
    public String deactivateUser(
            @RequestParam Long userId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminUserService.deactivateUser(userId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "회원이 비활성화되었습니다."
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

        return "redirect:/admin/users";
    }

    @PostMapping("/activate")
    public String activateUser(
            @RequestParam Long userId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminUserService.activateUser(userId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "회원이 활성화되었습니다."
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

        return "redirect:/admin/users";
    }
}