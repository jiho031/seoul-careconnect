package com.seoulcareconnect.service.admin;

import com.seoulcareconnect.dto.admin.AdminDashboardSummaryDTO;
import com.seoulcareconnect.dto.admin.AdminRecentUserDTO;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.PolicySource;
import com.seoulcareconnect.entity.policy.SyncLog;
import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.admin.AdminNoticeRepository;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import com.seoulcareconnect.repository.policy.PolicySourceRepository;
import com.seoulcareconnect.repository.policy.SyncLogRepository;
import com.seoulcareconnect.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.seoulcareconnect.dto.admin.AdminDailyStatusDTO;

import java.time.LocalDate;
import java.util.ArrayList;

import java.util.Comparator;
import java.util.List;


import com.seoulcareconnect.dto.admin.AdminNoticeDTO;
import com.seoulcareconnect.entity.admin.AdminNotice;
import com.seoulcareconnect.repository.admin.AdminNoticeRepository;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final PolicyRepository policyRepository;
    private final PolicySourceRepository policySourceRepository;
    private final SyncLogRepository syncLogRepository;
    private final UserRepository userRepository;
    private final AdminNoticeRepository adminNoticeRepository;

    public AdminDashboardSummaryDTO getSummary() {

        List<PolicySource> sources = policySourceRepository.findAll();
        List<User> users = userRepository.findAll();

        SyncLog latestLog = syncLogRepository.findAll()
                .stream()
                .filter(log -> log.getStartedAt() != null)
                .max(Comparator.comparing(SyncLog::getStartedAt))
                .orElse(null);

        long activeSourceCount = sources.stream()
                .filter(source -> Boolean.TRUE.equals(source.getIsActive()))
                .count();

        long activeUserCount = users.stream()
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .count();

        long latestSuccessCount = latestLog == null
                ? 0
                : safeCount(latestLog.getSuccessCount());

        long latestFailCount = latestLog == null
                ? 0
                : safeCount(latestLog.getFailCount());

        return AdminDashboardSummaryDTO.builder()
                .totalPolicyCount(policyRepository.count())
                .totalSourceCount(sources.size())
                .activeSourceCount(activeSourceCount)
                .latestCollectionSuccessCount(latestSuccessCount)
                .latestCollectionFailCount(latestFailCount)
                .totalUserCount(users.size())
                .activeUserCount(activeUserCount)
                .build();
    }

    public List<AdminRecentUserDTO> getRecentUsers() {

        return userRepository.findAll()
                .stream()
                .sorted(
                        Comparator.comparing(
                                User::getCreatedAt,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                )
                .limit(5)
                .map(this::toRecentUserDTO)
                .toList();
    }

    private AdminRecentUserDTO toRecentUserDTO(User user) {

        return AdminRecentUserDTO.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private long safeCount(Integer count) {
        return count == null ? 0 : count;
    }

    public List<AdminDailyStatusDTO> getRecentFiveDayStatus() {

        LocalDate today = LocalDate.now();

        List<Policy> allPolicies = policyRepository.findAll();
        List<SyncLog> allLogs = syncLogRepository.findAll();

        List<AdminDailyStatusDTO> result = new ArrayList<>();

        for (int i = 4; i >= 0; i--) {

            LocalDate targetDate = today.minusDays(i);

            long successCount = allPolicies.stream()
                    .filter(policy -> policy.getCreatedAt() != null)
                    .filter(policy ->
                            policy.getCreatedAt()
                                    .toLocalDate()
                                    .equals(targetDate)
                    )
                    .map(Policy::getPolicyId)
                    .distinct()
                    .count();

            long failCount = allLogs.stream()
                    .filter(log -> log.getStartedAt() != null)
                    .filter(log ->
                            log.getStartedAt()
                                    .toLocalDate()
                                    .equals(targetDate)
                    )
                    .mapToLong(log ->
                            safeCount(log.getFailCount())
                    )
                    .sum();

            long runCount = allLogs.stream()
                    .filter(log -> log.getStartedAt() != null)
                    .filter(log ->
                            log.getStartedAt()
                                    .toLocalDate()
                                    .equals(targetDate)
                    )
                    .count();

            result.add(
                    AdminDailyStatusDTO.builder()
                            .date(targetDate)
                            .successCount(successCount)
                            .failCount(failCount)
                            .runCount(runCount)
                            .build()
            );
        }

        return result;
    }

    public List<AdminNoticeDTO> getRecentNotices() {

        return adminNoticeRepository.findAll()
                .stream()
                .filter(notice ->
                        Boolean.TRUE.equals(notice.getIsVisible())
                )
                .sorted(
                        Comparator
                                .comparing(
                                        AdminNotice::getIsPinned,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                                .thenComparing(
                                        AdminNotice::getCreatedAt,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                )
                .limit(3)
                .map(this::toNoticeDTO)
                .toList();
    }

    private AdminNoticeDTO toNoticeDTO(AdminNotice notice) {

        String writerName = null;
        String writerEmail = null;

        if (notice.getCreatedBy() != null) {
            writerName = notice.getCreatedBy().getName();
            writerEmail = notice.getCreatedBy().getEmail();
        }

        return AdminNoticeDTO.builder()
                .noticeId(notice.getNoticeId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .noticeType(notice.getNoticeType())
                .pinned(notice.getIsPinned())
                .writerName(writerName)
                .writerEmail(writerEmail)
                .createdAt(notice.getCreatedAt())
                .build();
    }
}