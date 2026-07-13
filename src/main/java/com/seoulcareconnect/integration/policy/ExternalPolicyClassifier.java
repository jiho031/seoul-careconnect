package com.seoulcareconnect.integration.policy;

import com.seoulcareconnect.entity.policy.enums.AgeGroup;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ExternalPolicyClassifier {

    private static final List<String> SEOUL_DISTRICTS = List.of(
            "강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구",
            "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구",
            "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구"
    );

    private static final Pattern AGE_RANGE = Pattern.compile(
            "(?:만\\s*)?(\\d{1,3})\\s*세?\\s*(?:~|-|–|부터)\\s*(?:만\\s*)?(\\d{1,3})\\s*세"
    );
    private static final Pattern AGE_MIN = Pattern.compile(
            "(?:만\\s*)?(\\d{1,3})\\s*세\\s*(?:이상|초과|부터)"
    );
    private static final Pattern AGE_MAX = Pattern.compile(
            "(?:만\\s*)?(\\d{1,3})\\s*세\\s*(?:이하|미만|까지)"
    );

    public PolicyCategory category(PolicyCategory fallback, String... texts) {
        String value = combine(texts);

        if (contains(value, "주거", "주택", "임대", "전세", "월세", "분양", "주거비")) {
            return PolicyCategory.HOUSING;
        }
        if (contains(value, "돌봄", "간병", "보육", "요양", "가사 지원", "아이돌봄")) {
            return PolicyCategory.CARE;
        }
        if (contains(value, "교육", "훈련", "강좌", "과정", "세미나", "아카데미", "학습")) {
            return PolicyCategory.EDUCATION;
        }
        if (contains(value, "일자리", "취업", "고용", "창업", "구직", "직업", "재취업")) {
            return PolicyCategory.JOB;
        }
        if (contains(value, "문화", "여가", "체육", "관광", "공연", "예술")) {
            return PolicyCategory.CULTURE_LIFE;
        }
        if (contains(value, "생활", "생계", "지원금", "바우처", "건강", "의료", "복지", "1인가구")) {
            return PolicyCategory.LIVING_SUPPORT;
        }

        return fallback == null ? PolicyCategory.LIVING_SUPPORT : fallback;
    }

    public Set<AgeGroup> ageGroups(String... texts) {
        String value = combine(texts);
        EnumSet<AgeGroup> groups = EnumSet.noneOf(AgeGroup.class);

        Matcher rangeMatcher = AGE_RANGE.matcher(value);
        while (rangeMatcher.find()) {
            addOverlappingGroups(
                    groups,
                    Integer.parseInt(rangeMatcher.group(1)),
                    Integer.parseInt(rangeMatcher.group(2))
            );
        }

        Integer minAge = null;
        Matcher minMatcher = AGE_MIN.matcher(value);
        while (minMatcher.find()) {
            int found = Integer.parseInt(minMatcher.group(1));
            minAge = minAge == null ? found : Math.min(minAge, found);
        }

        Integer maxAge = null;
        Matcher maxMatcher = AGE_MAX.matcher(value);
        while (maxMatcher.find()) {
            int found = Integer.parseInt(maxMatcher.group(1));
            maxAge = maxAge == null ? found : Math.max(maxAge, found);
        }

        if (minAge != null || maxAge != null) {
            addOverlappingGroups(
                    groups,
                    minAge == null ? 0 : minAge,
                    maxAge == null ? 120 : maxAge
            );
        }

        if (contains(value, "10대", "20대", "30대", "청년", "대학생", "청소년", "아동", "영유아")) {
            groups.add(AgeGroup.UNDER_40);
        }
        if (contains(value, "40대")) groups.add(AgeGroup.FORTIES);
        if (contains(value, "50대")) groups.add(AgeGroup.FIFTIES);
        if (contains(value, "60대", "70대", "80대", "어르신", "노인", "노년", "고령자")) {
            groups.add(AgeGroup.SIXTIES_PLUS);
        }
        if (contains(value, "중장년", "중년")) {
            groups.add(AgeGroup.FORTIES);
            groups.add(AgeGroup.FIFTIES);
            groups.add(AgeGroup.SIXTIES_PLUS);
        }
        if (contains(value, "신중년")) {
            groups.add(AgeGroup.FIFTIES);
            groups.add(AgeGroup.SIXTIES_PLUS);
        }

        if (contains(value, "전 연령", "연령 제한 없음", "누구나", "모든 시민", "서울시민")) {
            return EnumSet.of(AgeGroup.ALL);
        }
        if (groups.size() == 4 || groups.isEmpty()) return EnumSet.of(AgeGroup.ALL);
        return groups;
    }

    public String ageGroupDisplay(String... texts) {
        Set<AgeGroup> groups = ageGroups(texts);
        if (groups.contains(AgeGroup.ALL)) return AgeGroup.ALL.getLabel();
        return groups.stream().map(AgeGroup::getLabel).reduce((a, b) -> a + ", " + b).orElse("전 연령");
    }

    public String searchableTarget(String rawTarget, String... extraTexts) {
        List<String> values = new ArrayList<>();
        String ageDisplay = ageGroupDisplay(rawTarget, combine(extraTexts));
        values.add(ageDisplay);

        String combined = combine(rawTarget, combine(extraTexts));
        for (String keyword : List.of("구직자", "1인가구", "저소득층", "소상공인", "장애인", "돌봄가구")) {
            if (matchesTargetKeyword(combined, keyword)) values.add(keyword);
        }

        if (rawTarget != null && !rawTarget.isBlank()) values.add(rawTarget.trim());
        return String.join(" | ", values.stream().distinct().toList());
    }

    public boolean matchesTargetKeyword(String text, String keyword) {
        String value = text == null ? "" : text;
        return switch (keyword) {
            case "구직자" -> contains(value, "구직자", "구직 중", "미취업", "실업자", "취업 준비", "재취업");
            case "1인가구" -> contains(value, "1인가구", "1인 가구", "독거");
            case "저소득층" -> contains(value, "저소득", "기초생활", "차상위", "생계급여", "중위소득");
            case "소상공인" -> contains(value, "소상공인", "자영업자", "중소기업", "사업자");
            case "장애인" -> contains(value, "장애인", "장애 정도", "장애 가구");
            case "돌봄가구" -> contains(value, "돌봄 가구", "보호자", "가족돌봄", "간병");
            default -> value.contains(keyword);
        };
    }

    public String district(String... texts) {
        String value = combine(texts);
        for (String district : SEOUL_DISTRICTS) {
            if (value.contains(district)) return district;
        }
        return null;
    }

    private void addOverlappingGroups(Set<AgeGroup> groups, int min, int max) {
        int start = Math.max(0, Math.min(min, max));
        int end = Math.max(min, max);
        if (start <= 39 && end >= 0) groups.add(AgeGroup.UNDER_40);
        if (start <= 49 && end >= 40) groups.add(AgeGroup.FORTIES);
        if (start <= 59 && end >= 50) groups.add(AgeGroup.FIFTIES);
        if (end >= 60) groups.add(AgeGroup.SIXTIES_PLUS);
    }

    private boolean contains(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private String combine(String... texts) {
        StringBuilder builder = new StringBuilder();
        if (texts != null) {
            for (String text : texts) {
                if (text != null && !text.isBlank()) {
                    if (!builder.isEmpty()) builder.append(' ');
                    builder.append(text.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return builder.toString();
    }
}
