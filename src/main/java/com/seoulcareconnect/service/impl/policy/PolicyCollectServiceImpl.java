package com.seoulcareconnect.service.impl.policy;

import com.seoulcareconnect.entity.policy.PolicySource;
import com.seoulcareconnect.entity.policy.SyncLog;
import com.seoulcareconnect.entity.policy.enums.SourceType;
import com.seoulcareconnect.entity.policy.enums.SyncStatus;
import com.seoulcareconnect.entity.policy.enums.SyncType;
import com.seoulcareconnect.integration.policy.ExternalPolicyClient;
import com.seoulcareconnect.integration.policy.ExternalPolicyItem;
import com.seoulcareconnect.integration.policy.SeoulPolicyFilter;
import com.seoulcareconnect.repository.policy.PolicySourceRepository;
import com.seoulcareconnect.repository.policy.SyncLogRepository;
import com.seoulcareconnect.service.policy.PolicyCollectService;
import com.seoulcareconnect.service.policy.PolicyCollectionSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyCollectServiceImpl implements PolicyCollectService {

    private final ObjectProvider<ExternalPolicyClient> clientProvider;
    private final PolicySourceRepository sourceRepository;
    private final SyncLogRepository syncLogRepository;
    private final PolicyUpsertService upsertService;
    private final SeoulPolicyFilter seoulPolicyFilter;

    private final AtomicBoolean collectionRunning = new AtomicBoolean(false);

    @Override
    public PolicyCollectionSummary collectAll(SyncType syncType) {
        if (!collectionRunning.compareAndSet(false, true)) {
            throw new IllegalStateException(
                    "정책 API 수집이 이미 진행 중입니다. 완료된 후 다시 시도해 주세요."
            );
        }

        List<PolicyCollectionSummary.SourceResult> results = new ArrayList<>();

        try {
            List<ExternalPolicyClient> clients =
                    clientProvider.orderedStream().toList();

            for (ExternalPolicyClient client : clients) {
                PolicyCollectionSummary.SourceResult result =
                        collectOne(client, syncType);

                results.add(result);
            }

            return new PolicyCollectionSummary(results);

        } finally {
            collectionRunning.set(false);
        }
    }

    @Override
    public void collectOne(String sourceName, SyncType syncType) {
        if (!collectionRunning.compareAndSet(false, true)) {
            throw new IllegalStateException(
                    "정책 API 수집이 이미 진행 중입니다. 완료된 후 다시 시도해 주세요."
            );
        }

        try {
            ExternalPolicyClient client = clientProvider.orderedStream()
                    .filter(candidate -> candidate.sourceName().equals(sourceName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "활성화된 API 수집기를 찾을 수 없습니다: " + sourceName
                    ));

            collectOne(client, syncType);

        } finally {
            collectionRunning.set(false);
        }
    }

    @Override
    public List<String> availableSourceNames() {
        return clientProvider.orderedStream()
                .map(ExternalPolicyClient::sourceName)
                .sorted()
                .toList();
    }

    private PolicyCollectionSummary.SourceResult collectOne(
            ExternalPolicyClient client,
            SyncType syncType
    ) {
        PolicySource source = getOrCreateSource(client);

        if (Boolean.FALSE.equals(source.getIsActive())) {
            log.info("{} 수집 출처가 비활성화되어 실행하지 않습니다.", client.sourceName());

            return new PolicyCollectionSummary.SourceResult(
                    client.sourceName(),
                    0,
                    0,
                    0,
                    0,
                    false
            );
        }

        SyncLog syncLog = new SyncLog();
        syncLog.setSource(source);
        syncLog.setSyncType(syncType);
        syncLog.setStatus(SyncStatus.SUCCESS);
        syncLog = syncLogRepository.save(syncLog);

        int candidateCount = 0;
        int success = 0;
        int filteredCount = 0;
        int duplicateCount = 0;
        int fail = 0;

        StringBuilder errors = new StringBuilder();

        try {
            List<ExternalPolicyItem> items = client.fetch();
            candidateCount = items.size();

            log.info("{} API 수집 결과: 후보 정책 {}건", client.sourceName(), candidateCount);

            if (items.isEmpty()) {
                throw new IllegalStateException(
                        client.sourceName() + " API 수집 결과가 0건입니다."
                );
            }

            for (ExternalPolicyItem item : items) {
                try {
                    if (!seoulPolicyFilter.shouldCollect(client.sourceName(), item)) {
                        filteredCount++;
                        continue;
                    }

                    boolean saved = upsertService.upsert(source, item);

                    if (saved) {
                        success++;
                    } else {
                        duplicateCount++;
                    }

                } catch (Exception itemError) {
                    fail++;

                    appendError(errors, itemError.getMessage());

                    log.warn(
                            "{} 정책 1건 저장 실패: externalId={}, title={}, 원인={}",
                            client.sourceName(),
                            item == null ? null : item.getExternalId(),
                            item == null ? null : item.getTitle(),
                            itemError.getMessage()
                    );
                }
            }

            log.info(
                    "{} 정책 처리 완료: 저장 {}건, 중복 {}건, 조건 제외 {}건, 실패 {}건",
                    client.sourceName(),
                    success,
                    duplicateCount,
                    filteredCount,
                    fail
            );

            if (success == 0
                    && fail == 0
                    && filteredCount > 0
                    && duplicateCount == 0) {
                log.warn(
                        "{} API 후보 {}건이 모두 서울·전국 대상 또는 신청기간 조건에서 제외되었습니다.",
                        client.sourceName(),
                        items.size()
                );
            }

            if (success == 0
                    && fail == 0
                    && duplicateCount > 0
                    && filteredCount == 0) {
                log.info(
                        "{} API 수집 대상 {}건이 모두 기존 정책과 중복되어 저장하지 않았습니다.",
                        client.sourceName(),
                        duplicateCount
                );
            }

        } catch (Exception clientError) {
            fail++;

            appendError(errors, clientError.getMessage());

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
        syncLog.setDuplicateCount(duplicateCount);
        syncLog.setStatus(resolveSyncStatus(success, fail));
        syncLog.setErrorMessage(
                errors.isEmpty()
                        ? null
                        : limit(errors.toString(), 4000)
        );
        syncLog.setEndedAt(LocalDateTime.now());

        syncLogRepository.save(syncLog);

        int totalSkipped = filteredCount + duplicateCount;

        return new PolicyCollectionSummary.SourceResult(
                client.sourceName(),
                candidateCount,
                success,
                totalSkipped,
                fail,
                true
        );
    }

    private SyncStatus resolveSyncStatus(int success, int fail) {
        if (fail == 0) {
            return SyncStatus.SUCCESS;
        }

        if (success == 0) {
            return SyncStatus.FAIL;
        }

        return SyncStatus.PARTIAL;
    }

    private PolicySource getOrCreateSource(ExternalPolicyClient client) {
        return sourceRepository
                .findFirstBySourceName(client.sourceName())
                .orElseGet(() -> {
                    PolicySource source = new PolicySource();

                    source.setSourceName(client.sourceName());
                    source.setSourceType(SourceType.OPEN_API);
                    source.setBaseUrl(client.sourceBaseUrl());
                    source.setApiKeyType("environment-variable");
                    source.setCategory(client.sourceCategory());
                    source.setIsActive(true);

                    return sourceRepository.save(source);
                });
    }

    private void appendError(StringBuilder errors, String message) {
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

    private String limit(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }

        return value.substring(0, max);
    }
}