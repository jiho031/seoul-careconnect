package com.seoulcareconnect.service.policy;

import java.util.List;

public record PolicyCollectionSummary(
        List<SourceResult> sourceResults
) {

    public PolicyCollectionSummary {
        sourceResults = sourceResults == null
                ? List.of()
                : List.copyOf(sourceResults);
    }

    public int totalCandidateCount() {
        return sourceResults.stream()
                .filter(SourceResult::executed)
                .mapToInt(SourceResult::candidateCount)
                .sum();
    }

    public int totalReflectedCount() {
        return sourceResults.stream()
                .filter(SourceResult::executed)
                .mapToInt(SourceResult::reflectedCount)
                .sum();
    }

    public int totalSkippedCount() {
        return sourceResults.stream()
                .filter(SourceResult::executed)
                .mapToInt(SourceResult::skippedCount)
                .sum();
    }

    public int totalFailedCount() {
        return sourceResults.stream()
                .filter(SourceResult::executed)
                .mapToInt(SourceResult::failedCount)
                .sum();
    }

    public String toLogText() {
        StringBuilder log = new StringBuilder();

        log.append("\n")
                .append("========== 정책 API 전체 수집 요약 ==========");

        if (sourceResults.isEmpty()) {
            log.append("\n실행된 정책 API가 없습니다.");
        }

        for (SourceResult result : sourceResults) {
            log.append("\n[").append(result.sourceName()).append("] ");

            if (!result.executed()) {
                log.append("비활성화되어 실행하지 않음");
                continue;
            }

            log.append("후보 ")
                    .append(result.candidateCount())
                    .append("건 / 반영 ")
                    .append(result.reflectedCount())
                    .append("건 / 제외 ")
                    .append(result.skippedCount())
                    .append("건 / 실패 ")
                    .append(result.failedCount())
                    .append("건 / ")
                    .append(resolveStatus(result));
        }

        log.append("\n")
                .append("------------------------------------------")
                .append("\n총합: 후보 ")
                .append(totalCandidateCount())
                .append("건 / 반영 ")
                .append(totalReflectedCount())
                .append("건 / 제외 ")
                .append(totalSkippedCount())
                .append("건 / 실패 ")
                .append(totalFailedCount())
                .append("건")
                .append("\n")
                .append("==========================================");

        return log.toString();
    }

    private String resolveStatus(SourceResult result) {
        if (result.failedCount() == 0) {
            return "성공";
        }

        if (result.reflectedCount() == 0) {
            return "실패";
        }

        return "부분 성공";
    }

    public record SourceResult(
            String sourceName,
            int candidateCount,
            int reflectedCount,
            int skippedCount,
            int failedCount,
            boolean executed
    ) {
    }
}