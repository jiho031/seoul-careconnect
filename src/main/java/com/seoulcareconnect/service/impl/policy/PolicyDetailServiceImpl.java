package com.seoulcareconnect.service.impl.policy;

import com.seoulcareconnect.dto.policy.PolicyDetailDTO;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyStatus;
import com.seoulcareconnect.mapper.policy.PolicyMapper;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import com.seoulcareconnect.service.policy.PolicyDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyDetailServiceImpl implements PolicyDetailService {

    private final PolicyRepository policyRepository;
    private final PolicyMapper policyMapper;

    @Value("${app.policy.sync.zone-id:Asia/Seoul}")
    private String zoneId;

    @Override
    public PolicyDetailDTO get(Long policyId) {
        Policy policy = policyRepository.findWithSourceAndDetailByPolicyId(policyId)
                .orElseThrow(this::notFound);

        boolean publicPolicy = policy.getStatus() == PolicyStatus.AUTO_PUBLISHED
                || policy.getStatus() == PolicyStatus.APPROVED;

        LocalDate today = LocalDate.now(ZoneId.of(zoneId));
        boolean active = policy.getApplyStatus() != ApplyStatus.EXPIRED
                && (policy.getStartDate() == null || !policy.getStartDate().isAfter(today))
                && (policy.getEndDate() == null || !policy.getEndDate().isBefore(today));

        if (!publicPolicy || !active) throw notFound();
        return policyMapper.toDetailDto(policy);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "정책을 찾을 수 없습니다.");
    }
}
