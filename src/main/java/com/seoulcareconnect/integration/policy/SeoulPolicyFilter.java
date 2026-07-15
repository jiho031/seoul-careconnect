package com.seoulcareconnect.integration.policy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class SeoulPolicyFilter {

    private static final List<String> SEOUL_BOUND_SOURCES = List.of(
            "서울시50플러스",
            "서울열린데이터",
            "서울 열린데이터",
            "마이홈",
            "고용24-국민내일배움카드",
            "고용24-사업주",
            "기업마당"
    );

    private static final List<String> NATIONAL_DEFAULT_SOURCES = List.of(
            "중앙부처복지서비스"
    );

    private static final List<String> SEOUL_WORDS = List.of(
            "서울특별시", "서울시", "서울"
    );

    private static final List<String> NATIONWIDE_WORDS = List.of(
            "전국", "전국민", "전 지역", "전지역",
            "지역무관", "지역 무관",
            "지역제한없음", "지역 제한 없음",
            "거주지 제한 없음", "전국 대상"
    );

    private static final List<String> OTHER_REGIONS = List.of(
            "경기도", "인천광역시", "부산광역시",
            "대구광역시", "광주광역시", "대전광역시",
            "울산광역시", "세종특별자치시",
            "강원특별자치도", "강원도",
            "충청북도", "충청남도",
            "전북특별자치도", "전라북도", "전라남도",
            "경상북도", "경상남도",
            "제주특별자치도", "제주도"
    );

    @Value("${app.policy.collect.seoul-only:true}")
    private boolean seoulOnly;

    @Value("${app.policy.collect.include-nationwide:true}")
    private boolean includeNationwide;

    public boolean shouldCollect(
            String sourceName,
            ExternalPolicyItem item
    ) {
        if (!seoulOnly) return true;
        if (item == null) return false;

        String explicitRegion = join(
                item.getRegion(),
                item.getDistrict()
        );

        if (containsAny(explicitRegion, SEOUL_WORDS)) return true;

        if (includeNationwide
                && containsAny(explicitRegion, NATIONWIDE_WORDS)) {
            return true;
        }

        if (containsAny(explicitRegion, OTHER_REGIONS)) return false;

        if (containsAny(sourceName, SEOUL_BOUND_SOURCES)) return true;

        String fullText = join(
                item.getTitle(),
                item.getSummary(),
                item.getTarget(),
                item.getRegion(),
                item.getDistrict(),
                item.getBenefit(),
                item.getSelectionCriteria(),
                item.getContentText(),
                item.getApplyMethod(),
                item.getContact()
        );

        if (containsAny(fullText, SEOUL_WORDS)) return true;

        if (includeNationwide
                && containsAny(fullText, NATIONWIDE_WORDS)) {
            return true;
        }

        if (containsAny(fullText, OTHER_REGIONS)) return false;

        return includeNationwide
                && containsAny(sourceName, NATIONAL_DEFAULT_SOURCES);
    }

    public String resolveRegion(
            String sourceName,
            ExternalPolicyItem item
    ) {
        if (item == null) return null;

        if (containsAny(item.getRegion(), SEOUL_WORDS)
                || containsAny(item.getDistrict(), SEOUL_WORDS)
                || containsAny(sourceName, SEOUL_BOUND_SOURCES)) {
            return "서울특별시";
        }

        String fullText = join(
                item.getRegion(),
                item.getDistrict(),
                item.getTitle(),
                item.getSummary(),
                item.getTarget(),
                item.getBenefit(),
                item.getSelectionCriteria(),
                item.getContentText()
        );

        if ((includeNationwide
                && containsAny(fullText, NATIONWIDE_WORDS))
                || containsAny(sourceName, NATIONAL_DEFAULT_SOURCES)) {
            return "전국";
        }

        return clean(item.getRegion());
    }

    private boolean containsAny(String value, List<String> keywords) {
        String normalized = normalize(value);
        if (normalized == null) return false;

        return keywords.stream()
                .map(this::normalize)
                .anyMatch(normalized::contains);
    }

    private String join(String... values) {
        StringBuilder result = new StringBuilder();

        for (String value : values) {
            String cleaned = clean(value);
            if (cleaned == null) continue;

            if (!result.isEmpty()) result.append(' ');
            result.append(cleaned);
        }

        return result.isEmpty() ? null : result.toString();
    }

    private String clean(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private String normalize(String value) {
        String cleaned = clean(value);
        if (cleaned == null) return null;

        return cleaned
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "");
    }
}