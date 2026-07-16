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
public class PolicyExpirationService {

    private final PolicyRepository policyRepository;

    @Value("${app.policy.sync.zone-id:Asia/Seoul}")
    private String zoneId;

    @Value("${app.policy.query.closing-soon-days:7}")
    private int closingSoonDays;

    @Transactional
    public int refreshStatuses() {
        ZoneId zone = ZoneId.of(zoneId);
        LocalDate today = LocalDate.now(zone);
        LocalDate closingDate = today.plusDays(Math.max(closingSoonDays, 0));
        LocalDateTime now = LocalDateTime.now(zone);

        int changed = 0;
        changed += policyRepository.expireEndedPolicies(today, now);
        changed += policyRepository.reopenPoliciesOutsideClosingWindow(closingDate, now);
        changed += policyRepository.markClosingSoonPolicies(today, closingDate, now);
        return changed;
    }
}
