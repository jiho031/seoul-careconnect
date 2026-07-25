package com.seoulcareconnect.service.impl.ai;

import com.seoulcareconnect.dto.ai.AiAssistantUserContext;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.PolicyDetail;
import com.seoulcareconnect.entity.policy.enums.AgeGroup;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class PolicyQuestionFilter {

    private static final List<String> SEOUL_DISTRICTS = List.of(
            "강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구",
            "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구",
            "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구"
    );
    private static final Map<PolicyCategory, List<String>> CATEGORY_KEYWORDS = Map.of(
            PolicyCategory.EDUCATION, List.of("교육", "훈련", "강좌", "배움", "자격증"),
            PolicyCategory.JOB, List.of("일자리", "취업", "구직", "직업", "창업", "채용"),
            PolicyCategory.HOUSING, List.of("주거", "주택", "월세", "전세", "임대", "이사"),
            PolicyCategory.LIVING_SUPPORT, List.of("생활", "생계", "지원금", "바우처", "금융"),
            PolicyCategory.CARE, List.of("돌봄", "간병", "보육", "가족", "요양"),
            PolicyCategory.CULTURE_LIFE, List.of("문화", "여가", "체육", "관광", "예술")
    );
    private static final Set<String> STOP_WORDS = Set.of(
            "서울", "서울시", "정책", "지원", "신청", "관련", "있는", "없는", "알려줘",
            "알려주세요", "찾아줘", "찾아주세요", "쉽게", "확인", "도와줘", "도와주세요"
    );
    private static final Pattern TOKEN_SEPARATOR = Pattern.compile("[^0-9a-zA-Z가-힣]+");

    public boolean matches(String question, Policy policy) {
        String normalized = normalize(question);
        String district = detectDistrict(normalized);
        if (district != null && !matchesDistrict(district, policy)) {
            return false;
        }

        Set<PolicyCategory> categories = detectCategories(normalized);
        if (!categories.isEmpty() && !categories.contains(policy.getCategory())) {
            return false;
        }

        AgeGroup ageGroup = detectAgeGroup(normalized);
        return ageGroup == null || matchesAgeGroup(ageGroup, policy.getTarget());
    }

    public String enrichWithProfile(
            String question,
            AiAssistantUserContext userContext
    ) {
        StringBuilder enriched = new StringBuilder(question.trim());
        String normalized = normalize(question);

        if (userContext.hasDistrict() && detectDistrict(normalized) == null) {
            enriched.append(" 사용자 지역: ").append(userContext.district().trim());
        }
        if (userContext.hasAgeGroup() && detectAgeGroup(normalized) == null) {
            enriched.append(" 사용자 연령대: ").append(userContext.ageGroup().trim());
        }
        return enriched.toString();
    }

    public int lexicalScore(String question, Policy policy) {
        String[] tokens = TOKEN_SEPARATOR.split(normalize(question));
        String title = normalize(policy.getTitle());
        String target = normalize(policy.getTarget());
        String method = normalize(policy.getApplyMethod());
        PolicyDetail detail = policy.getDetail();
        String content = normalize(detail == null ? null : detail.getContentText());
        String documents = normalize(detail == null ? null : detail.getRequiredDocumentsText());

        int score = 0;
        for (String token : tokens) {
            if (token.length() < 2 || STOP_WORDS.contains(token)) continue;
            if (title.contains(token)) score += 8;
            if (target.contains(token)) score += 5;
            if (method.contains(token)) score += 3;
            if (documents.contains(token)) score += 3;
            if (content.contains(token)) score += 2;
        }

        if (detectDistrict(normalize(question)) != null) score += 6;
        if (!detectCategories(normalize(question)).isEmpty()) score += 5;
        if (detectAgeGroup(normalize(question)) != null) score += 5;
        return score;
    }

    private String detectDistrict(String question) {
        return SEOUL_DISTRICTS.stream()
                .filter(question::contains)
                .findFirst()
                .orElse(null);
    }

    private Set<PolicyCategory> detectCategories(String question) {
        EnumSet<PolicyCategory> categories = EnumSet.noneOf(PolicyCategory.class);
        CATEGORY_KEYWORDS.forEach((category, keywords) -> {
            if (keywords.stream().anyMatch(question::contains)) {
                categories.add(category);
            }
        });
        return categories;
    }

    private AgeGroup detectAgeGroup(String question) {
        if (containsAny(question, "60대", "70대", "80대", "65세", "노년", "어르신", "노인")) {
            return AgeGroup.SIXTIES_PLUS;
        }
        if (containsAny(question, "50대", "50세")) {
            return AgeGroup.FIFTIES;
        }
        if (containsAny(question, "40대", "40세")) {
            return AgeGroup.FORTIES;
        }
        if (containsAny(question, "청년", "20대", "30대", "39세 이하")) {
            return AgeGroup.UNDER_40;
        }
        return null;
    }

    private boolean matchesDistrict(String district, Policy policy) {
        String policyDistrict = normalize(policy.getDistrict());
        if (policyDistrict.isEmpty()
                || policyDistrict.equals("서울시 전체")
                || policyDistrict.equals("서울 전체")) {
            return true;
        }
        return policyDistrict.equals(district);
    }

    private boolean matchesAgeGroup(AgeGroup ageGroup, String targetValue) {
        String target = normalize(targetValue);
        if (target.isEmpty()
                || containsAny(target, "전 연령", "연령 무관", "누구나", "전체", "서울시민")) {
            return true;
        }

        if (ageGroup == AgeGroup.SIXTIES_PLUS) {
            return containsAny(target, "60대", "65세", "노년", "어르신", "노인", "중장년");
        }
        if (ageGroup == AgeGroup.FIFTIES) {
            return containsAny(target, "50대", "50세", "중장년");
        }
        if (ageGroup == AgeGroup.FORTIES) {
            return containsAny(target, "40대", "40세", "중장년");
        }
        return containsAny(target, "청년", "20대", "30대", "39세");
    }

    private boolean containsAny(String text, String... candidates) {
        return Arrays.stream(candidates).anyMatch(text::contains);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) return "";
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
