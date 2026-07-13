package com.seoulcareconnect.service.policy;

public interface PolicyViewLogService {
    void record(Long policyId, Long userId, String userAgent);
}