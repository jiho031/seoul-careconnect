package com.seoulcareconnect.config.policy;

import com.seoulcareconnect.entity.policy.enums.SyncType;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import com.seoulcareconnect.service.impl.policy.PolicyCleanupService;
import com.seoulcareconnect.service.impl.policy.PolicyExpirationService;
import com.seoulcareconnect.service.policy.PolicyCollectService;
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
    private final PolicyRepository policyRepository;

    @Override
    public void run(ApplicationArguments args) {
        long policyCount = policyRepository.count();

        if (policyCount == 0) {
            log.info("정책 데이터가 없어 최초 API 수집을 시작합니다.");
            policyCollectService.collectAll(SyncType.MANUAL);
        } else {
            log.info(
                    "정책 데이터가 이미 {}건 존재하여 API 수집을 생략합니다.",
                    policyCount
            );
        }

        expirationService.refreshStatuses();
        cleanupService.hideOldExpiredPolicies();
    }
}