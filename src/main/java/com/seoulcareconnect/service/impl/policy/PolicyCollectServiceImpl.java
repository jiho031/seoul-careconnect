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
import com.seoulcareconnect.integration.policy.SeoulPolicyFilter;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private final ObjectProvider<ExternalPolicyClient> clientProvider;
    private final PolicySourceRepository sourceRepository;
    private final SyncLogRepository syncLogRepository;
    private final PolicyUpsertService upsertService;
    private final SeoulPolicyFilter seoulPolicyFilter;
    private final AtomicBoolean collectionRunning = new AtomicBoolean(false);

    @Override
    public void collectAll(SyncType syncType) {

        // 이미 다른 수집 작업이 실행 중이면 중복 실행하지 않는다.
        if (!collectionRunning.compareAndSet(false, true)) {
            throw new IllegalStateException(
                    "정책 API 수집이 이미 진행 중입니다. 완료된 후 다시 시도해 주세요."
            );
        }

        try {
            List<ExternalPolicyClient> clients =
                    clientProvider.orderedStream().toList();

            for (ExternalPolicyClient client : clients) {
                collectOne(client, syncType);
            }

        } finally {
            // 성공하거나 오류가 발생해도 반드시 실행 상태를 해제한다.
            collectionRunning.set(false);
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
                    if (!seoulPolicyFilter.shouldCollect(
                            client.sourceName(),
                            item
                    )) {
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
                                + "서울·전국 대상 또는 신청기간 "
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