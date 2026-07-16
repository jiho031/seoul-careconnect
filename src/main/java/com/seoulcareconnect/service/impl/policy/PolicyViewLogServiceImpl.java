package com.seoulcareconnect.service.impl.policy;

import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.PolicyViewLog;
import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyStatus;
import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import com.seoulcareconnect.repository.policy.PolicyViewLogRepository;
import com.seoulcareconnect.repository.user.UserRepository;
import com.seoulcareconnect.service.policy.PolicyViewLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PolicyViewLogServiceImpl implements PolicyViewLogService {

    private final PolicyRepository policyRepository;
    private final PolicyViewLogRepository policyViewLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void record(Long policyId, Long userId, String userAgent) {
        if (userId == null) {
            throw new IllegalArgumentException("로그인 회원 정보가 필요합니다.");
        }

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("정책을 찾을 수 없습니다."));

        boolean publicPolicy = policy.getStatus() == PolicyStatus.AUTO_PUBLISHED
                || policy.getStatus() == PolicyStatus.APPROVED;
        if (!publicPolicy || policy.getApplyStatus() == ApplyStatus.EXPIRED) return;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        policyRepository.increaseViewCount(policyId);

        PolicyViewLog log = new PolicyViewLog();
        log.setPolicy(policy);
        log.setUser(user);
        log.setUserAgent(limit(userAgent, 500));
        policyViewLogRepository.save(log);
    }

    private String limit(String value, int max) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isBlank()) return null;
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
