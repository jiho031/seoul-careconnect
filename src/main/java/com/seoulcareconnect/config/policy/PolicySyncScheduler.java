package com.seoulcareconnect.config.policy;

import com.seoulcareconnect.entity.policy.enums.SyncType;
import com.seoulcareconnect.service.impl.policy.PolicyCleanupService;
import com.seoulcareconnect.service.impl.policy.PolicyExpirationService;
import com.seoulcareconnect.service.policy.PolicyCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.policy.sync.scheduler-enabled",
        havingValue = "true"
)
public class PolicySyncScheduler {

    private final PolicyCollectService policyCollectService;
    private final PolicyExpirationService expirationService;
    private final PolicyCleanupService cleanupService;

    @Scheduled(
            cron = "${app.policy.sync.cron:0 0 2 * * *}",
            zone = "${app.policy.sync.zone-id:Asia/Seoul}"
    )
    public void syncPolicies() {
        try {
            policyCollectService.collectAll(SyncType.SCHEDULED);
        } catch (RuntimeException e) {
            log.error("정책 API 수집 중 오류가 발생했습니다.", e);
        }

        try {
            expirationService.refreshStatuses();
        } catch (RuntimeException e) {
            log.error("마감 정책 처리 중 오류가 발생했습니다.", e);
        }

        try {

            int deletedCount =
                    cleanupService.deleteOldExpiredPolicies();

            if (deletedCount > 0) {

                log.info(
                        "보관 기간이 지난 마감 정책 {}건을 DB에서 삭제했습니다.",
                        deletedCount
                );
            }

        } catch (RuntimeException e) {

            log.error(
                    "오래된 마감 정책 삭제 중 오류가 발생했습니다.",
                    e
            );
        }
    }
}
