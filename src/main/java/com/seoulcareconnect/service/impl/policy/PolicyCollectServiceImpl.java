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
public class PolicyCollectServiceImpl
        implements PolicyCollectService {

    private final ObjectProvider<ExternalPolicyClient> clientProvider;
    private final PolicySourceRepository sourceRepository;
    private final SyncLogRepository syncLogRepository;
    private final PolicyUpsertService upsertService;
    private final SeoulPolicyFilter seoulPolicyFilter;

    private final AtomicBoolean collectionRunning =
            new AtomicBoolean(false);

    @Override
    public PolicyCollectionSummary collectAll(
            SyncType syncType
    ) {
        /*
         * 다른 수집 작업이 실행 중이면
         * 중복 실행하지 않는다.
         */
        if (!collectionRunning.compareAndSet(false, true)) {
            throw new IllegalStateException(
                    "정책 API 수집이 이미 진행 중입니다. "
                            + "완료된 후 다시 시도해 주세요."
            );
        }

        List<PolicyCollectionSummary.SourceResult> results =
                new ArrayList<>();

        try {
            List<ExternalPolicyClient> clients =
                    clientProvider
                            .orderedStream()
                            .toList();

            for (ExternalPolicyClient client : clients) {
                PolicyCollectionSummary.SourceResult result =
                        collectOne(
                                client,
                                syncType
                        );

                results.add(result);
            }

            return new PolicyCollectionSummary(results);

        } finally {
            /*
             * 성공하거나 오류가 발생해도
             * 반드시 실행 상태를 해제한다.
             */
            collectionRunning.set(false);
        }
    }

    private PolicyCollectionSummary.SourceResult collectOne(
            ExternalPolicyClient client,
            SyncType syncType
    ) {
        PolicySource source =
                getOrCreateSource(client);

        /*
         * 비활성화된 출처는 수집하지 않는다.
         */
        if (Boolean.FALSE.equals(source.getIsActive())) {
            log.info(
                    "{} 수집 출처가 비활성화되어 실행하지 않습니다.",
                    client.sourceName()
            );

            return new PolicyCollectionSummary.SourceResult(
                    client.sourceName(),
                    0,
                    0,
                    0,
                    0,
                    false
            );
        }

        /*
         * 수집 로그 생성
         */
        SyncLog syncLog = new SyncLog();

        syncLog.setSource(source);
        syncLog.setSyncType(syncType);
        syncLog.setStatus(SyncStatus.SUCCESS);

        syncLog =
                syncLogRepository.save(syncLog);

        /*
         * 수집 결과 집계
         */
        int candidateCount = 0;

        // 실제 신규 저장 건수
        int success = 0;

        // 서울·전국 대상, 신청기간 등 조건에서 제외된 건수
        int filteredCount = 0;

        // 기존 정책이라 저장하지 않은 중복 건수
        int duplicateCount = 0;

        // 처리 중 오류가 발생한 건수
        int fail = 0;

        StringBuilder errors =
                new StringBuilder();

        try {
            List<ExternalPolicyItem> items =
                    client.fetch();

            candidateCount =
                    items.size();

            log.info(
                    "{} API 수집 결과: 후보 정책 {}건",
                    client.sourceName(),
                    candidateCount
            );

            if (items.isEmpty()) {
                throw new IllegalStateException(
                        client.sourceName()
                                + " API 수집 결과가 0건입니다."
                );
            }

            for (ExternalPolicyItem item : items) {
                try {
                    /*
                     * 서울·전국 대상 여부나
                     * 신청기간 조건을 만족하지 않으면 제외한다.
                     */
                    if (!seoulPolicyFilter.shouldCollect(
                            client.sourceName(),
                            item
                    )) {
                        filteredCount++;
                        continue;
                    }

                    /*
                     * 신규 정책이면 true,
                     * 이미 존재하는 정책이면 false를 반환한다.
                     */
                    boolean saved =
                            upsertService.upsert(
                                    source,
                                    item
                            );

                    if (saved) {
                        success++;
                    } else {
                        duplicateCount++;
                    }

                } catch (Exception itemError) {
                    fail++;

                    appendError(
                            errors,
                            itemError.getMessage()
                    );

                    log.warn(
                            "{} 정책 1건 저장 실패: "
                                    + "externalId={}, "
                                    + "title={}, "
                                    + "원인={}",
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
                            + "저장 {}건, "
                            + "중복 {}건, "
                            + "조건 제외 {}건, "
                            + "실패 {}건",
                    client.sourceName(),
                    success,
                    duplicateCount,
                    filteredCount,
                    fail
            );

            /*
             * 신규 저장도 실패도 없고,
             * 조건 제외만 발생한 경우 안내 로그를 남긴다.
             */
            if (success == 0
                    && fail == 0
                    && filteredCount > 0
                    && duplicateCount == 0) {

                log.warn(
                        "{} API 후보 {}건이 모두 "
                                + "서울·전국 대상 또는 "
                                + "신청기간 조건에서 제외되었습니다.",
                        client.sourceName(),
                        items.size()
                );
            }

            /*
             * 신규 저장도 실패도 없고,
             * 모든 수집 대상이 중복인 경우 안내 로그를 남긴다.
             */
            if (success == 0
                    && fail == 0
                    && duplicateCount > 0
                    && filteredCount == 0) {

                log.info(
                        "{} API 수집 대상 {}건이 모두 "
                                + "기존 정책과 중복되어 저장하지 않았습니다.",
                        client.sourceName(),
                        duplicateCount
                );
            }

        } catch (Exception clientError) {
            /*
             * API 호출이나 전체 처리 과정에서
             * 오류가 발생한 경우
             */
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

        /*
         * API 출처 마지막 확인 시각 갱신
         */
        source.setLastCheckedAt(
                LocalDateTime.now()
        );

        sourceRepository.save(source);

        /*
         * 수집 로그에 집계 결과 저장
         */
        syncLog.setSuccessCount(success);
        syncLog.setFailCount(fail);
        syncLog.setDuplicateCount(
                duplicateCount
        );

        syncLog.setStatus(
                resolveSyncStatus(
                        success,
                        fail
                )
        );

        syncLog.setErrorMessage(
                errors.isEmpty()
                        ? null
                        : limit(
                        errors.toString(),
                        4000
                )
        );

        syncLog.setEndedAt(
                LocalDateTime.now()
        );

        syncLogRepository.save(syncLog);

        /*
         * 기존 SourceResult 구조에서는
         * 조건 제외와 중복을 하나의 skipped 값으로 전달한다.
         */
        int totalSkipped =
                filteredCount + duplicateCount;

        return new PolicyCollectionSummary.SourceResult(
                client.sourceName(),
                candidateCount,
                success,
                totalSkipped,
                fail,
                true
        );
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

                    return sourceRepository.save(
                            source
                    );
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

        return value.substring(
                0,
                max
        );
    }
}