package com.seoulcareconnect.controller.admin;

import com.seoulcareconnect.service.admin.AdminNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminNoticeController {

    private final AdminNoticeService adminNoticeService;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/admin/notices")
    public String createNotice(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(defaultValue = "NORMAL") String noticeType,
            @RequestParam(defaultValue = "false") Boolean pinned,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminNoticeService.createNotice(
                    title,
                    content,
                    noticeType,
                    pinned,
                    authentication
            );

            redirectAttributes.addFlashAttribute(
                    "noticeSuccess",
                    "관리자 공지가 등록되었습니다."
            );

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "noticeError",
                    e.getMessage() == null
                            ? "공지 등록 중 오류가 발생했습니다."
                            : e.getMessage()
            );
        }

        return "redirect:/admin/dashboard";
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/admin/notices/{noticeId}/edit")
    public String updateNotice(
            @PathVariable Long noticeId,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(defaultValue = "NORMAL") String noticeType,
            @RequestParam(defaultValue = "false") Boolean pinned,
            RedirectAttributes redirectAttributes
    ) {
        adminNoticeService.updateNotice(
                noticeId,
                title,
                content,
                noticeType,
                pinned
        );

        redirectAttributes.addFlashAttribute(
                "noticeSuccess",
                "관리자 공지가 수정되었습니다."
        );

        return "redirect:/admin/dashboard";
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/admin/notices/{noticeId}/delete")
    public String deleteNotice(
            @PathVariable Long noticeId,
            RedirectAttributes redirectAttributes
    ) {
        adminNoticeService.deleteNotice(noticeId);

        redirectAttributes.addFlashAttribute(
                "noticeSuccess",
                "관리자 공지가 삭제되었습니다."
        );

        return "redirect:/admin/dashboard";
    }
}