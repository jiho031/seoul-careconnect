package com.seoulcareconnect.service.admin;

import com.seoulcareconnect.dto.admin.AdminCollectionSummaryDTO;
import com.seoulcareconnect.dto.admin.AdminSourceStatusDTO;
import com.seoulcareconnect.dto.admin.AdminSyncLogDTO;
import com.seoulcareconnect.entity.policy.PolicySource;
import com.seoulcareconnect.entity.policy.SyncLog;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import com.seoulcareconnect.repository.policy.PolicySourceRepository;
import com.seoulcareconnect.repository.policy.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCollectionService {

    private final PolicyRepository policyRepository;
    private final PolicySourceRepository policySourceRepository;
    private final SyncLogRepository syncLogRepository;

    /**
     * API 수집 현황 페이지 상단 통계
     */
    public AdminCollectionSummaryDTO getSummary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        List<SyncLog> todayLogs = syncLogRepository.findAll()
                .stream()
                .filter(log -> log.getStartedAt() != null)
                .filter(log ->
                        !log.getStartedAt().isBefore(todayStart)
                                && log.getStartedAt().isBefore(tomorrowStart)
                )
                .toList();

        long successCount = todayLogs.stream()
                .mapToLong(log -> safeCount(log.getSuccessCount()))
                .sum();

        long failCount = todayLogs.stream()
                .mapToLong(log -> safeCount(log.getFailCount()))
                .sum();

        long requestedCount = successCount + failCount;

        List<PolicySource> sources = policySourceRepository.findAll();

        long activeSourceCount = sources.stream()
                .filter(source -> Boolean.TRUE.equals(source.getIsActive()))
                .count();

        return AdminCollectionSummaryDTO.builder()
                .todayRequestedCount(requestedCount)
                .todaySuccessCount(successCount)
                .reviewRequiredCount(0)
                .todayFailCount(failCount)
                .totalSourceCount(sources.size())
                .activeSourceCount(activeSourceCount)
                .totalPolicyCount(policyRepository.count())
                .build();
    }

    /**
     * API 수집처별 연동 상태
     */
    public List<AdminSourceStatusDTO> getSourceStatuses() {
        List<PolicySource> sources = policySourceRepository.findAll();

        Map<Long, SyncLog> latestLogMap = syncLogRepository.findAll()
                .stream()
                .filter(log -> log.getSource() != null)
                .filter(log -> log.getSource().getSourceId() != null)
                .sorted(
                        Comparator.comparing(
                                SyncLog::getStartedAt,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                )
                .collect(
                        Collectors.toMap(
                                log -> log.getSource().getSourceId(),
                                Function.identity(),
                                (first, ignored) -> first
                        )
                );

        return sources.stream()
                .sorted(
                        Comparator.comparing(
                                PolicySource::getSourceName,
                                Comparator.nullsLast(
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                )
                .map(source -> toSourceStatusDTO(
                        source,
                        latestLogMap.get(source.getSourceId())
                ))
                .toList();
    }

    /**
     * 최근 수집 로그 20건
     */
    public List<AdminSyncLogDTO> getRecentSyncLogs() {
        return syncLogRepository.findAll()
                .stream()
                .sorted(
                        Comparator.comparing(
                                SyncLog::getStartedAt,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                )
                .limit(20)
                .map(this::toSyncLogDTO)
                .toList();
    }

    private AdminSourceStatusDTO toSourceStatusDTO(
            PolicySource source,
            SyncLog latestLog
    ) {
        return AdminSourceStatusDTO.builder()
                .sourceId(source.getSourceId())
                .sourceName(source.getSourceName())
                .sourceType(source.getSourceType())
                .baseUrl(source.getBaseUrl())
                .active(Boolean.TRUE.equals(source.getIsActive()))
                .lastCheckedAt(source.getLastCheckedAt())
                .latestSyncStatus(
                        latestLog == null
                                ? null
                                : latestLog.getStatus()
                )
                .latestStartedAt(
                        latestLog == null
                                ? null
                                : latestLog.getStartedAt()
                )
                .latestEndedAt(
                        latestLog == null
                                ? null
                                : latestLog.getEndedAt()
                )
                .latestSuccessCount(
                        latestLog == null
                                ? 0
                                : safeCount(latestLog.getSuccessCount())
                )
                .latestFailCount(
                        latestLog == null
                                ? 0
                                : safeCount(latestLog.getFailCount())
                )
                .latestErrorMessage(
                        latestLog == null
                                ? null
                                : latestLog.getErrorMessage()
                )
                .build();
    }

    private AdminSyncLogDTO toSyncLogDTO(SyncLog log) {
        PolicySource source = log.getSource();

        return AdminSyncLogDTO.builder()
                .logId(log.getLogId())
                .sourceId(
                        source == null
                                ? null
                                : source.getSourceId()
                )
                .sourceName(
                        source == null
                                ? "출처 정보 없음"
                                : source.getSourceName()
                )
                .syncType(log.getSyncType())
                .status(log.getStatus())
                .successCount(safeCount(log.getSuccessCount()))
                .failCount(safeCount(log.getFailCount()))
                .errorMessage(log.getErrorMessage())
                .startedAt(log.getStartedAt())
                .endedAt(log.getEndedAt())
                .build();
    }

    private int safeCount(Integer count) {
        return count == null ? 0 : count;
    }
}