package com.seoulcareconnect.service.impl.policy;

import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyStatus;
import com.seoulcareconnect.repository.ai.AiPolicyExplanationRepository;
import com.seoulcareconnect.repository.policy.PolicyCollectionErrorRepository;
import com.seoulcareconnect.repository.policy.PolicyDetailRepository;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import com.seoulcareconnect.repository.policy.PolicyViewLogRepository;
import com.seoulcareconnect.repository.report.MissingPolicyReportRepository;
import com.seoulcareconnect.repository.user.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyCleanupService {

    private final PolicyRepository policyRepository;
    private final PolicyDetailRepository policyDetailRepository;
    private final PolicyViewLogRepository policyViewLogRepository;
    private final AiPolicyExplanationRepository aiPolicyExplanationRepository;
    private final MissingPolicyReportRepository missingPolicyReportRepository;
    private final PolicyCollectionErrorRepository policyCollectionErrorRepository;
    private final FavoriteRepository favoriteRepository;

    @Value("${app.policy.sync.zone-id:Asia/Seoul}")
    private String zoneId;

    @Value("${app.policy.cleanup.enabled:true}")
    private boolean enabled;

    @Value("${app.policy.cleanup.delete-after-days:30}")
    private int deleteAfterDays;

    @Transactional
    public int deleteOldExpiredPolicies() {

        if (!enabled) {
            return 0;
        }

        ZoneId zone =
                ZoneId.of(zoneId);

        LocalDate deleteBefore =
                LocalDate.now(zone)
                        .minusDays(
                                Math.max(
                                        deleteAfterDays,
                                        0
                                )
                        );

        List<Long> policyIds =
                policyRepository.findOldExpiredPolicyIds(
                        deleteBefore,
                        ApplyStatus.EXPIRED,
                        List.of(
                                PolicyStatus.EXPIRED,
                                PolicyStatus.HIDDEN
                        )
                );

        if (policyIds.isEmpty()) {
            return 0;
        }

        // 관심 정책
        favoriteRepository.deleteByPolicyIds(
                policyIds
        );

        // 정책 조회 기록
        policyViewLogRepository.deleteByPolicyIds(
                policyIds
        );

        // AI 정책 설명
        aiPolicyExplanationRepository.deleteByPolicyIds(
                policyIds
        );

        // 정책 상세 정보
        policyDetailRepository.deleteByPolicyIds(
                policyIds
        );

        missingPolicyReportRepository
                .clearPolicyReferences(
                        policyIds
                );

        policyCollectionErrorRepository
                .clearPolicyReferences(
                        policyIds
                );


        policyRepository.deleteAllByIdInBatch(
                policyIds
        );


        return policyIds.size();
    }
}