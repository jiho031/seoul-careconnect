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

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 회원 관리 목록 화면
     */
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

        // 회원 목록
        model.addAttribute(
                "userPage",
                userPage
        );

        model.addAttribute(
                "users",
                userPage.getContent()
        );

        // 상단 통계
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

        // 검색 조건 유지
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

    /**
     * 회원 비활성화
     */
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

    /**
     * 회원 활성화
     */
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