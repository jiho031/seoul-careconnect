package com.seoulcareconnect.service.impl.policy;

import com.seoulcareconnect.repository.policy.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class PolicyCleanupService {

    private final PolicyRepository policyRepository;

    @Value("${app.policy.sync.zone-id:Asia/Seoul}")
    private String zoneId;

    @Value("${app.policy.cleanup.enabled:true}")
    private boolean enabled;

    @Value("${app.policy.cleanup.hide-after-days:30}")
    private int hideAfterDays;

    @Transactional
    public int hideOldExpiredPolicies() {
        if (!enabled) return 0;

        ZoneId zone = ZoneId.of(zoneId);
        LocalDate hideBefore = LocalDate.now(zone).minusDays(Math.max(hideAfterDays, 0));
        return policyRepository.hideOldExpiredPolicies(
                hideBefore,
                LocalDateTime.now(zone)
        );
    }
}
