package com.seoulcareconnect.service.admin;

import com.seoulcareconnect.repository.admin.AdminActivityLogRepository;
import com.seoulcareconnect.repository.policy.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogCleanupService {

    private final SyncLogRepository syncLogRepository;
    private final AdminActivityLogRepository adminActivityLogRepository;

    @Scheduled(
            cron = "0 0 3 * * *",
            zone = "Asia/Seoul"
    )
    @Transactional
    public void cleanupOldLogs() {
        LocalDateTime cutoffDateTime =
                LocalDateTime.now().minusDays(14);

        long deletedSyncLogCount =
                syncLogRepository.deleteByStartedAtBefore(
                        cutoffDateTime
                );

        long deletedActivityLogCount =
                adminActivityLogRepository.deleteByCreatedAtBefore(
                        cutoffDateTime
                );

        log.info(
                "14일 이전 로그 정리 완료: 수집 로그 {}건, 관리자 활동 로그 {}건 삭제",
                deletedSyncLogCount,
                deletedActivityLogCount
        );
    }
}