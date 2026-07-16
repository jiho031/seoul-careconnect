package com.seoulcareconnect.config.policy;

import com.seoulcareconnect.entity.policy.enums.SyncType;
import com.seoulcareconnect.service.impl.policy.PolicyCleanupService;
import com.seoulcareconnect.service.impl.policy.PolicyExpirationService;
import com.seoulcareconnect.service.policy.PolicyCollectService;
import com.seoulcareconnect.service.policy.PolicyCollectionSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.policy.sync.run-on-startup",
        havingValue = "true"
)
public class PolicyStartupRunner implements ApplicationRunner {

    private final PolicyCollectService policyCollectService;
    private final PolicyExpirationService expirationService;
    private final PolicyCleanupService cleanupService;

    @Override
    public void run(ApplicationArguments args) {

        log.info("서버 시작 정책 API 전체 수집을 실행합니다.");

        PolicyCollectionSummary summary =
                policyCollectService.collectAll(
                        SyncType.MANUAL
                );

        expirationService.refreshStatuses();

        cleanupService.hideOldExpiredPolicies();

        log.info("{}", summary.toLogText());

        log.info(
                "서버 시작 정책 API 전체 수집을 완료했습니다."
        );
    }
}