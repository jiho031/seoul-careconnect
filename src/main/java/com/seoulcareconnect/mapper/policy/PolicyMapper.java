package com.seoulcareconnect.mapper.policy;

import com.seoulcareconnect.dto.policy.PolicyDTO;
import com.seoulcareconnect.dto.policy.PolicyDetailDTO;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.PolicyDetail;
import com.seoulcareconnect.entity.policy.enums.AgeGroup;
import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.integration.policy.ExternalPolicyClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class PolicyMapper {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final ExternalPolicyClassifier classifier;

    public PolicyMapper(ExternalPolicyClassifier classifier) {
        this.classifier = classifier;
    }

    @Value("${app.policy.sync.zone-id:Asia/Seoul}")
    private String zoneId;

    @Value("${app.policy.query.new-days:14}")
    private int newDays;

    @Value("${app.policy.query.dday-priority-days:14}")
    private int dDayPriorityDays;

    public PolicyDTO toDto(Policy policy) {
        PolicyDetail detail = policy.getDetail();
        LocalDate today = today();

        return PolicyDTO.builder()
                .policyId(policy.getPolicyId())
                .title(valueOr(policy.getTitle(), "제목 없음"))
                .agency(resolveAgency(policy))
                .summary(resolveSummary(policy, detail))
                .category(policy.getCategory() == null
                        ? null
                        : policy.getCategory().name())
                .categoryLabel(categoryLabel(policy.getCategory()))
                .target(valueOr(displayTarget(policy.getTarget()), "공식 공고 확인"))
                .ageGroupDisplay(ageGroupDisplay(policy.getTarget()))
                .regionDisplay(regionDisplay(policy))
                .applyStatus(policy.getApplyStatus() == null
                        ? null
                        : policy.getApplyStatus().name())
                .applyStatusLabel(
                        displayApplyStatusLabel(policy)
                )
                .applyPeriod(applyPeriod(
                        policy.getStartDate(),
                        policy.getEndDate(),
                        policy.getApplyStatus()
                ))
                .applyMethod(valueOr(
                        policy.getApplyMethod(),
                        "공식 공고 확인"
                ))
                .dDayLabel(dDayLabel(
                        policy.getEndDate(),
                        policy.getApplyStatus(),
                        today
                ))
                .dDayCssClass(dDayCssClass(
                        policy.getEndDate(),
                        today
                ))
                .dDayPriority(isDdayPriority(
                        policy.getEndDate(),
                        today
                ))
                .createdDate(policy.getCreatedAt() == null
                        ? "-"
                        : policy.getCreatedAt()
                        .toLocalDate()
                        .format(DATE_FORMAT))
                .viewCount(policy.getViewCount())
                .tags(tags(policy))
                .newPolicy(policy.getCreatedAt() != null
                        && !policy.getCreatedAt()
                        .toLocalDate()
                        .isBefore(today.minusDays(
                                Math.max(newDays, 0)
                        )))
                .build();
    }

    public PolicyDetailDTO toDetailDto(Policy policy) {
        PolicyDetail detail = policy.getDetail();
        String benefit = detail == null
                ? null
                : normalizeBlock(detail.getBenefit());
        String documents = detail == null
                ? null
                : normalizeBlock(detail.getRequiredDocumentsText());
        String application = normalizeBlock(policy.getApplyMethod());
        String content = detail == null
                ? null
                : normalizeBlock(detail.getContentText());
        String selectionCriteria = detail == null
                ? null
                : normalizeBlock(detail.getSelectionCriteria());

        if (isKStartupPolicy(policy)) {
            selectionCriteria = null;
        }

        return PolicyDetailDTO.builder()
                .policyId(policy.getPolicyId())
                .title(valueOr(policy.getTitle(), "제목 없음"))
                .agency(resolveAgency(policy))
                .summary(resolveSummary(policy, detail))
                .sourceName(policy.getSource() == null
                        ? "공식 제공기관"
                        : valueOr(
                        policy.getSource().getSourceName(),
                        "공식 제공기관"
                ))
                .categoryLabel(categoryLabel(policy.getCategory()))
                .ageGroupDisplay(ageGroupDisplay(policy.getTarget()))
                .target(valueOr(
                        displayTarget(policy.getTarget()),
                        "지원 대상은 공식 공고에서 확인해 주세요."
                ))
                .regionDisplay(regionDisplay(policy))
                .applyStatusLabel(displayApplyStatusLabel(policy))
                .dDayLabel(dDayLabel(
                        policy.getEndDate(),
                        policy.getApplyStatus(),
                        today()
                ))
                .periodLabel(periodLabel(policy))
                .applyPeriod(displayPeriod(policy))
                .applyMethod(valueOr(
                        application,
                        "신청 방법은 공식 공고에서 확인해 주세요."
                ))
                .officialUrl(safeUrl(policy.getOfficialUrl()))
                .contact(normalizeInline(policy.getContact()))
                .benefit(valueOr(
                        benefit,
                        resolveSummary(policy, detail)
                ))
                .selectionCriteria(selectionCriteria)
                .requiredDocumentsText(documents)
                .contentText(content)
                .benefitLines(splitLines(benefit))
                .documentLines(splitLines(documents))
                .applicationLines(splitLines(application))
                .contentLines(splitLines(content))
                .build();
    }


    public String categoryLabel(PolicyCategory category) {
        return category == null ? "생활지원" : category.getLabel();
    }

    public String applyStatusLabel(ApplyStatus status) {
        if (status == null) return "공고 확인";
        return status.getLabel();
    }

    private String resolveAgency(Policy policy) {
        String contact = normalizeInline(policy.getContact());
        if (contact != null) {
            int separator = contact.indexOf(" | ");
            String first = separator >= 0 ? contact.substring(0, separator) : contact;
            if (!first.matches("^[0-9\\-\\s,()]+$") && first.length() <= 100) {
                return first;
            }
        }
        if (policy.getSource() != null) {
            String sourceName = normalizeInline(policy.getSource().getSourceName());
            if (sourceName != null) return sourceName;
        }
        return "공식 제공기관";
    }

    private String displayApplyStatusLabel(Policy policy) {
        ApplyStatus status = policy == null
                ? null
                : policy.getApplyStatus();

        if (status != ApplyStatus.INFORMATION_ONLY) {
            return applyStatusLabel(status);
        }

        String sourceName = sourceName(policy);

        if ("고용24-국민내일배움카드훈련".equals(sourceName)
                || "고용24-사업주훈련".equals(sourceName)) {
            return "훈련 정보 확인";
        }

        if ("고용24-구직자취업역량강화프로그램".equals(sourceName)) {
            return "프로그램 정보 확인";
        }

        return applyStatusLabel(status);
    }

    private String resolveSummary(
            Policy policy,
            PolicyDetail detail
    ) {
        if (isMyHomePolicy(policy)) {
            String agency = resolveAgency(policy);
            String title = valueOr(
                    policy.getTitle(),
                    "주거 지원 공고"
            );

            return shorten(
                    agency
                            + "에서 진행하는 "
                            + title
                            + "입니다. 세부 자격과 신청 조건은 공식 공고에서 확인해 주세요.",
                    150
            );
        }

        String value = null;

        if (detail != null) {
            value = firstNonBlank(
                    detail.getBenefit(),
                    detail.getContentText()
            );
        }

        value = firstNonBlank(
                value,
                policy.getTarget()
        );

        return shorten(
                valueOr(
                        value,
                        "상세 내용은 공식 공고를 확인해 주세요."
                ),
                150
        );
    }

    private boolean isKStartupPolicy(Policy policy) {
        return "K-Startup-지원사업공고".equals(sourceName(policy));
    }

    private boolean isMyHomePolicy(
            Policy policy
    ) {
        if (policy.getSource() == null) {
            return false;
        }

        String sourceName =
                policy.getSource().getSourceName();

        return sourceName != null
                && sourceName.startsWith("마이홈-");
    }

    private String ageGroupDisplay(String target) {
        String value = normalizeInline(target);
        if (value == null) return AgeGroup.ALL.getLabel();
        return classifier.ageGroupDisplay(value);
    }

    private String displayTarget(String target) {
        String value = normalizeInline(target);
        if (value == null) return null;

        int separator = value.indexOf(" | ");
        if (separator < 0) return value;

        String firstSegment = value.substring(0, separator);

        boolean generatedAgePrefix = firstSegment.matches(
                "(?:30대 이하|40대|50대|60대 이상|전 연령)"
                        + "(?:, (?:30대 이하|40대|50대|60대 이상))*"
        );

        if (!generatedAgePrefix) return value;

        return classifier.ageGroupDisplay(value)
                + value.substring(separator);
    }

    private String regionDisplay(Policy policy) {
        String region = normalizeInline(policy.getRegion());
        String district = normalizeInline(policy.getDistrict());
        if (district != null) return valueOr(region, "서울") + " " + district;
        if (region == null || "서울".equals(region) || "서울특별시".equals(region)) return "서울시 전체";
        return region;
    }

    private String periodLabel(Policy policy) {
        String sourceName = sourceName(policy);

        if ("고용24-국민내일배움카드훈련".equals(sourceName)
                || "고용24-사업주훈련".equals(sourceName)) {
            return "훈련 기간";
        }

        if ("고용24-구직자취업역량강화프로그램".equals(sourceName)) {
            return "프로그램 기간";
        }

        return "신청 기간";
    }

    private String displayPeriod(Policy policy) {
        String sourceName = sourceName(policy);

        boolean operationPeriodSource =
                "고용24-국민내일배움카드훈련".equals(sourceName)
                        || "고용24-사업주훈련".equals(sourceName)
                        || "고용24-구직자취업역량강화프로그램".equals(sourceName);

        /*
         * 고용24 날짜는 신청기간이 아니라 훈련 또는 프로그램
         * 운영기간이므로 INFORMATION_ONLY여도 날짜를 표시합니다.
         */
        if (operationPeriodSource
                && (policy.getStartDate() != null
                || policy.getEndDate() != null)) {
            return dateRange(
                    policy.getStartDate(),
                    policy.getEndDate()
            );
        }

        return applyPeriod(
                policy.getStartDate(),
                policy.getEndDate(),
                policy.getApplyStatus()
        );
    }

    private String dateRange(
            LocalDate start,
            LocalDate end
    ) {
        if (start == null && end == null) {
            return "공식 안내 확인";
        }

        if (start == null) {
            return "~ " + end.format(DATE_FORMAT);
        }

        if (end == null) {
            return start.format(DATE_FORMAT) + " ~";
        }

        if (start.equals(end)) {
            return start.format(DATE_FORMAT);
        }

        return start.format(DATE_FORMAT)
                + " ~ "
                + end.format(DATE_FORMAT);
    }

    private String sourceName(Policy policy) {
        if (policy == null || policy.getSource() == null) {
            return null;
        }

        return normalizeInline(
                policy.getSource().getSourceName()
        );
    }

    private String applyPeriod(LocalDate start, LocalDate end, ApplyStatus status) {
        if (status == ApplyStatus.INFORMATION_ONLY) return "공식 안내 확인";
        if (status == ApplyStatus.ALWAYS && start == null && end == null) return "상시 신청";
        if (start == null && end == null) return "공고 확인";
        if (start == null) return "~ " + end.format(DATE_FORMAT);
        if (end == null) return start.format(DATE_FORMAT) + " ~";
        if (start.equals(end)) return start.format(DATE_FORMAT);
        return start.format(DATE_FORMAT) + " ~ " + end.format(DATE_FORMAT);
    }

    private String dDayLabel(LocalDate endDate, ApplyStatus status, LocalDate today) {
        if (status == ApplyStatus.INFORMATION_ONLY) return "안내";
        if (status == ApplyStatus.ALWAYS) return "상시";
        if (endDate == null) return "공고 확인";
        long days = ChronoUnit.DAYS.between(today, endDate);
        if (days < 0) return "마감";
        if (days == 0) return "D-DAY";
        return "D-" + days;
    }

    private String dDayCssClass(LocalDate endDate, LocalDate today) {
        if (endDate == null) return "blue";
        long days = ChronoUnit.DAYS.between(today, endDate);
        if (days <= 7) return "red";
        if (days <= 14) return "orange";
        return "teal";
    }

    private boolean isDdayPriority(
            LocalDate endDate,
            LocalDate today
    ) {
        if (endDate == null) {
            return false;
        }

        long remainingDays =
                ChronoUnit.DAYS.between(today, endDate);

        return remainingDays >= 0
                && remainingDays
                <= Math.max(dDayPriorityDays, 0);
    }

    private List<String> tags(Policy policy) {
        Set<String> values = new LinkedHashSet<>();
        values.add(ageGroupDisplay(policy.getTarget()));
        values.add(categoryLabel(policy.getCategory()));
        if (normalizeInline(policy.getDistrict()) != null) values.add(policy.getDistrict());
        else values.add(regionDisplay(policy));

        String target = normalizeInline(policy.getTarget());
        if (target != null) {
            for (String keyword : List.of("구직자", "1인가구", "저소득층", "소상공인", "장애인", "돌봄가구")) {
                if (target.contains(keyword)) values.add(keyword);
            }
        }

        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .limit(4)
                .toList();
    }

    private List<String> splitLines(String text) {
        String value = normalizeBlock(text);

        if (value == null) {
            return new ArrayList<>();
        }

        return Arrays.stream(
                        value.split("(?:\\r?\\n)+|[•▪■]+")
                )
                .map(line -> line
                        .replaceFirst(
                                "^\\s*(?:[-–—]\\s+|\\d{1,2}[.)]\\s+)",
                                ""
                        )
                        .replaceFirst(
                                "^\\s*(?:~{1,3}|※)\\s*",
                                ""
                        )
                        .trim()
                )
                .filter(line -> !line.isBlank())
                .distinct()
                .limit(30)
                .toList();
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of(zoneId));
    }

    private String safeUrl(String url) {
        String value = normalizeInline(url);
        if (value == null) return null;
        return value.startsWith("https://") || value.startsWith("http://") ? value : null;
    }

    private String normalizeInline(String value) {
        if (value == null) return null;
        String result = value.replaceAll("\\s+", " ").trim();
        return result.isBlank() ? null : result;
    }

    private String normalizeBlock(String value) {
        if (value == null) return null;
        String result = value
                .replaceAll("[ \\t\\r\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        return result.isBlank() ? null : result;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String cleaned = normalizeBlock(value);
            if (cleaned != null) return cleaned;
        }
        return null;
    }

    private String valueOr(String value, String fallback) {
        String cleaned = normalizeInline(value);
        return cleaned == null ? fallback : cleaned;
    }

    private String shorten(String value, int maxLength) {
        String cleaned = normalizeInline(value);
        if (cleaned == null || cleaned.length() <= maxLength) return cleaned;
        return cleaned.substring(0, maxLength) + "...";
    }
}
