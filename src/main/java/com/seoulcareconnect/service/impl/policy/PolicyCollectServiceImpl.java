package com.seoulcareconnect.service.impl.policy;

import com.seoulcareconnect.entity.policy.PolicySource;
import com.seoulcareconnect.entity.policy.SyncLog;
import com.seoulcareconnect.entity.policy.enums.SourceType;
import com.seoulcareconnect.entity.policy.enums.SyncStatus;
import com.seoulcareconnect.entity.policy.enums.SyncType;
import com.seoulcareconnect.integration.policy.ExternalPolicyClient;
import com.seoulcareconnect.integration.policy.ExternalPolicyItem;
import com.seoulcareconnect.repository.policy.PolicySourceRepository;
import com.seoulcareconnect.repository.policy.SyncLogRepository;
import com.seoulcareconnect.service.policy.PolicyCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyCollectServiceImpl
        implements PolicyCollectService {

    private static final String LOCAL_WELFARE_SOURCE_NAME =
            "공공데이터포털-지자체복지서비스";

    private final ObjectProvider<ExternalPolicyClient> clientProvider;
    private final PolicySourceRepository sourceRepository;
    private final SyncLogRepository syncLogRepository;
    private final PolicyUpsertService upsertService;

    @Override
    public void collectAll(SyncType syncType) {
        List<ExternalPolicyClient> clients =
                clientProvider.orderedStream().toList();

        for (ExternalPolicyClient client : clients) {
            collectOne(client, syncType);
        }
    }

    private void collectOne(
            ExternalPolicyClient client,
            SyncType syncType
    ) {
        PolicySource source = getOrCreateSource(client);

        if (Boolean.FALSE.equals(source.getIsActive())) {
            log.info(
                    "{} 수집 출처가 비활성화되어 실행하지 않습니다.",
                    client.sourceName()
            );

            return;
        }

        SyncLog syncLog = new SyncLog();

        syncLog.setSource(source);
        syncLog.setSyncType(syncType);
        syncLog.setStatus(SyncStatus.SUCCESS);

        syncLog = syncLogRepository.save(syncLog);

        int success = 0;
        int skipped = 0;
        int fail = 0;

        StringBuilder errors = new StringBuilder();

        try {
            List<ExternalPolicyItem> items =
                    client.fetch();

            log.info(
                    "{} API 수집 결과: 후보 정책 {}건",
                    client.sourceName(),
                    items.size()
            );

            if (items.isEmpty()) {
                throw new IllegalStateException(
                        client.sourceName()
                                + " API 수집 결과가 0건입니다."
                );
            }

            for (ExternalPolicyItem item : items) {
                try {
                    if (shouldSkipByRegion(client, item)) {
                        skipped++;
                        continue;
                    }

                    boolean saved =
                            upsertService.upsert(
                                    source,
                                    item
                            );

                    if (saved) {
                        success++;
                    } else {
                        skipped++;
                    }

                } catch (Exception itemError) {
                    fail++;

                    appendError(
                            errors,
                            itemError.getMessage()
                    );

                    log.warn(
                            "{} 정책 1건 저장 실패: "
                                    + "externalId={}, title={}, 원인={}",
                            client.sourceName(),
                            item == null
                                    ? null
                                    : item.getExternalId(),
                            item == null
                                    ? null
                                    : item.getTitle(),
                            itemError.getMessage()
                    );
                }
            }

            log.info(
                    "{} 정책 처리 완료: "
                            + "저장 {}건, 제외 {}건, 실패 {}건",
                    client.sourceName(),
                    success,
                    skipped,
                    fail
            );

            if (success == 0
                    && fail == 0
                    && skipped > 0) {

                log.warn(
                        "{} API 후보 {}건이 모두 "
                                + "서울 지역 또는 신청기간 "
                                + "조건에서 제외되었습니다.",
                        client.sourceName(),
                        items.size()
                );
            }

        } catch (Exception clientError) {
            fail++;

            appendError(
                    errors,
                    clientError.getMessage()
            );

            log.error(
                    "{} API 수집 실패: {}",
                    client.sourceName(),
                    clientError.getMessage(),
                    clientError
            );
        }

        source.setLastCheckedAt(LocalDateTime.now());
        sourceRepository.save(source);

        syncLog.setSuccessCount(success);
        syncLog.setFailCount(fail);

        syncLog.setStatus(
                resolveSyncStatus(success, fail)
        );

        syncLog.setErrorMessage(
                errors.isEmpty()
                        ? null
                        : limit(errors.toString(), 4000)
        );

        syncLog.setEndedAt(LocalDateTime.now());

        syncLogRepository.save(syncLog);
    }

    // 서울특별시만 걸러내는 필터
    private boolean shouldSkipByRegion(
            ExternalPolicyClient client,
            ExternalPolicyItem item
    ) {
        if (!LOCAL_WELFARE_SOURCE_NAME.equals(
                client.sourceName()
        )) {
            return false;
        }

        return !isSeoulPolicy(item);
    }

    private boolean isSeoulPolicy(
            ExternalPolicyItem item
    ) {
        if (item == null) {
            return false;
        }

        String region =
                normalizeRegion(item.getRegion());

        return "서울".equals(region)
                || "서울특별시".equals(region)
                || (
                region != null
                        && region.startsWith("서울특별시")
        );
    }

    private String normalizeRegion(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value
                .replaceAll("\\s+", "")
                .trim();

        return normalized.isBlank()
                ? null
                : normalized;
    }

    private SyncStatus resolveSyncStatus(
            int success,
            int fail
    ) {
        if (fail == 0) {
            return SyncStatus.SUCCESS;
        }

        if (success == 0) {
            return SyncStatus.FAIL;
        }

        return SyncStatus.PARTIAL;
    }

    private PolicySource getOrCreateSource(
            ExternalPolicyClient client
    ) {
        return sourceRepository
                .findFirstBySourceName(
                        client.sourceName()
                )
                .orElseGet(() -> {
                    PolicySource source =
                            new PolicySource();

                    source.setSourceName(
                            client.sourceName()
                    );

                    source.setSourceType(
                            SourceType.OPEN_API
                    );

                    source.setBaseUrl(
                            client.sourceBaseUrl()
                    );

                    source.setApiKeyType(
                            "application.properties"
                    );

                    source.setCategory(
                            client.sourceCategory()
                    );

                    source.setIsActive(true);

                    return sourceRepository.save(source);
                });
    }

    private void appendError(
            StringBuilder errors,
            String message
    ) {
        if (errors.length() >= 4000) {
            return;
        }

        if (!errors.isEmpty()) {
            errors.append("\n");
        }

        errors.append(
                message == null
                        ? "알 수 없는 수집 오류"
                        : message
        );
    }

    private String limit(
            String value,
            int max
    ) {
        if (value == null
                || value.length() <= max) {
            return value;
        }

        return value.substring(0, max);
    }
}