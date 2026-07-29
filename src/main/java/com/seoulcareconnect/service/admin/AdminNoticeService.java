package com.seoulcareconnect.service.admin;

import com.seoulcareconnect.entity.admin.AdminNotice;
import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.admin.AdminNoticeRepository;
import com.seoulcareconnect.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminNoticeService {

    private final AdminNoticeRepository adminNoticeRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createNotice(
            String title,
            String content,
            String noticeType,
            Boolean pinned,
            Authentication authentication
    ) {
        String email = authentication.getName();

        User admin = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "관리자 정보를 찾을 수 없습니다."
                        )
                );

        if (!"SUPER_ADMIN".equalsIgnoreCase(admin.getRole())) {
            throw new IllegalStateException(
                    "최고 관리자만 공지를 등록할 수 있습니다."
            );
        }

        AdminNotice notice = new AdminNotice();

        notice.setTitle(title.trim());
        notice.setContent(content.trim());
        notice.setNoticeType(normalizeNoticeType(noticeType));
        notice.setIsPinned(Boolean.TRUE.equals(pinned));
        notice.setIsVisible(true);
        notice.setCreatedBy(admin);

        adminNoticeRepository.save(notice);
    }

    private String normalizeNoticeType(String noticeType) {
        if (noticeType == null) {
            return "NORMAL";
        }

        return switch (noticeType.toUpperCase()) {
            case "IMPORTANT" -> "IMPORTANT";
            case "SECURITY" -> "SECURITY";
            default -> "NORMAL";
        };
    }

    @Transactional
    public void updateNotice(
            Long noticeId,
            String title,
            String content,
            String noticeType,
            Boolean pinned
    ) {
        AdminNotice notice = adminNoticeRepository.findById(noticeId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "공지 정보를 찾을 수 없습니다."
                        )
                );

        notice.setTitle(title.trim());
        notice.setContent(content.trim());
        notice.setNoticeType(normalizeNoticeType(noticeType));
        notice.setIsPinned(Boolean.TRUE.equals(pinned));
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        AdminNotice notice = adminNoticeRepository.findById(noticeId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "공지 정보를 찾을 수 없습니다."
                        )
                );

        adminNoticeRepository.delete(notice);
    }
}