package com.seoulcareconnect.config.policy;

import com.seoulcareconnect.entity.policy.enums.SyncType;
import com.seoulcareconnect.service.impl.policy.PolicyCleanupService;
import com.seoulcareconnect.service.impl.policy.PolicyExpirationService;
import com.seoulcareconnect.service.policy.PolicyCollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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
        policyCollectService.collectAll(SyncType.MANUAL);
        expirationService.refreshStatuses();
        cleanupService.hideOldExpiredPolicies();
    }
}
