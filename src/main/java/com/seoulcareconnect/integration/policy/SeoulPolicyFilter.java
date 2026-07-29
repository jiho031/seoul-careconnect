package com.seoulcareconnect.integration.policy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class SeoulPolicyFilter {

    private static final List<String> BLOCKED_SOURCES = List.of(
            "고용24-국민내일배움카드훈련",
            "고용24-사업주훈련"
    );

    private static final List<String> SEOUL_BOUND_SOURCES = List.of(
            "서울시50플러스",
            "서울열린데이터",
            "서울 열린데이터",
            "마이홈"
    );

    private static final List<String> TRUSTED_PUBLIC_EDUCATION_SOURCES = List.of(
            "서울시50플러스",
            "고용24-구직자취업역량강화프로그램"
    );

    private static final List<String> AD_EDUCATION_PROVIDER_WORDS = List.of(
            "학원",
            "아카데미",
            "캠퍼스",
            "평생교육원",
            "교육원",
            "직업전문학교",
            "직업훈련기관",
            "러닝센터",
            "연수원"
    );

    private static final List<String> AD_EDUCATION_COURSE_WORDS = List.of(
            "강좌",
            "교육과정",
            "훈련과정",
            "실무과정",
            "양성과정",
            "수강생 모집",
            "교육생 모집",
            "수강신청",
            "개강",
            "부트캠프",
            "자격증 과정",
            "특강",
            "세미나",
            "웨비나",
            "원데이클래스",
            "원데이 클래스"
    );

    private static final List<String> YOUTH_ONLY_TITLE_WORDS = List.of(
            "청년층",
            "청년",
            "청소년",
            "아동",
            "고등학생",
            "중학생",
            "초등학생",
            "대학생",
            "학교 밖 청소년",
            "학교밖청소년"
    );

    private static final List<String> YOUTH_ONLY_TARGET_WORDS = List.of(
            "만34세 이하",
            "만 34세 이하",
            "만39세 이하",
            "만 39세 이하",
            "30대 이하",
            "청년 전용",
            "청년전용",
            "청년 구직자",
            "청년구직자"
    );

    private static final List<String> MIDAGE_WORDS = List.of(
            "중장년",
            "신중년",
            "중년",
            "4050",
            "5060",
            "40대",
            "50대",
            "60대",
            "40세 이상",
            "만 40세 이상",
            "50세 이상",
            "만 50세 이상"
    );

    // K-Startup의 biz_trgt_age에는 여러 연령대가 함께 들어오는 경우가 있어
    // 단순히 "만 40세 이상"이 있다는 이유만으로 중장년 맞춤 공고로 판단하지 않는다.
    private static final List<String> MIDAGE_STRONG_WORDS = List.of(
            "중장년",
            "신중년",
            "중년",
            "4050",
            "5060",
            "40대",
            "50대",
            "60대"
    );

    private static final List<String> KSTARTUP_SOURCES = List.of(
            "K-Startup"
    );

    private static final List<String> KSTARTUP_PERSONAL_SUPPORT_WORDS = List.of(
            "예비창업",
            "1인 창조기업",
            "1인창조기업",
            "1인 기업",
            "1인기업",
            "개인사업자",
            "재창업",
            "폐업자",
            "창업지원센터",
            "창업 지원센터",
            "창업멘토링",
            "창업 멘토링",
            "입주기업"
    );

    private static final List<String> BUSINESS_EVENT_WORDS = List.of(
            "오픈이노베이션",
            "데모데이",
            "전시회",
            "박람회",
            "라운드테이블",
            "투자유치",
            "해외진출",
            "해외 진출",
            "글로벌진출",
            "글로벌 진출",
            "IR 데모",
            "IR피칭",
            "IR 피칭",
            "참가기업",
            "참여기업",
            "수혜기업",
            "실증 프로그램",
            "R&D 우수성과",
            "우수성과 50선",
            "기술이전"
    );

    private static final List<String> BUSINESS_NON_SEOUL_LOCATION_WORDS = List.of(
            "부산",
            "대구",
            "인천",
            "광주",
            "대전",
            "울산",
            "세종",
            "경기도",
            "판교",
            "수원",
            "성남",
            "용인",
            "고양",
            "강원",
            "충북",
            "충남",
            "전북",
            "전남",
            "경북",
            "경남",
            "제주"
    );

    private static final List<String> BIZINFO_SOURCES = List.of(
            "기업마당"
    );

    private static final List<String> BIZINFO_CORE_WORDS = List.of(
            "중장년",
            "신중년",
            "시니어",
            "4050",
            "5060",
            "40대",
            "50대",
            "60대",
            "소상공인",
            "자영업",
            "자영업자",
            "개인사업자",
            "1인기업",
            "1인 기업",
            "전통시장",
            "골목상권",
            "재창업",
            "재기지원",
            "재기 지원",
            "폐업자",
            "폐업 소상공인",
            "재취업",
            "전직",
            "경력전환",
            "경력 전환",
            "취업지원",
            "취업 지원",
            "고용지원",
            "고용 지원"
    );

    private static final List<String> BIZINFO_PERSONAL_STARTUP_WORDS = List.of(
            "예비창업",
            "창업자",
            "1인 창조기업",
            "1인창조기업",
            "1인기업",
            "개인사업자",
            "IP디딤돌"
    );

    private static final List<String> NATIONAL_DEFAULT_SOURCES = List.of(
            "중앙부처복지서비스",
            "K-Startup",
            "기업마당"
    );

    private static final List<String> SEOUL_WORDS = List.of(
            "서울특별시",
            "서울시",
            "서울"
    );

    private static final List<String> SEOUL_DISTRICT_WORDS = List.of(
            "강남구",
            "강동구",
            "강북구",
            "관악구",
            "광진구",
            "구로구",
            "금천구",
            "노원구",
            "도봉구",
            "동대문구",
            "동작구",
            "마포구",
            "서대문구",
            "서초구",
            "성동구",
            "성북구",
            "송파구",
            "양천구",
            "영등포구",
            "용산구",
            "은평구",
            "종로구",
            "중랑구"
    );

    private static final List<String> NATIONWIDE_WORDS = List.of(
            "전국",
            "전국민",
            "전 지역",
            "전지역",
            "지역무관",
            "지역 무관",
            "지역제한없음",
            "지역 제한 없음",
            "거주지 제한 없음",
            "전국 대상"
    );

    private static final List<String> OTHER_REGIONS = List.of(
            "경기도",
            "인천광역시",
            "부산광역시",
            "대구광역시",
            "광주광역시",
            "대전광역시",
            "울산광역시",
            "세종특별자치시",
            "강원특별자치도",
            "강원도",
            "충청북도",
            "충청남도",
            "전북특별자치도",
            "전라북도",
            "전라남도",
            "경상북도",
            "경상남도",
            "제주특별자치도",
            "제주도"
    );

    @Value("${app.policy.collect.seoul-only:true}")
    private boolean seoulOnly;

    @Value("${app.policy.collect.include-nationwide:true}")
    private boolean includeNationwide;

    public boolean shouldCollect(
            String sourceName,
            ExternalPolicyItem item
    ) {
        if (item == null) {
            return false;
        }

        // 강사 피드백으로 제거하기로 한 고용24 훈련 API는
        // 설정이 실수로 다시 켜져도 저장하지 않는다.
        if (containsAny(sourceName, BLOCKED_SOURCES)) {
            return false;
        }

        // 중장년 중심 서비스이므로 제목/대상이 명확한 청년·아동 전용 정책은 제외한다.
        if (isClearlyYouthOnly(item)) {
            return false;
        }

        // 민간 학원·아카데미·캠퍼스의 개별 강좌성 데이터 제외.
        // 단, 서울시50플러스와 고용24 취업역량 프로그램은 공공 핵심 데이터이므로 예외 처리한다.
        if (isAdvertisementLikeEducation(sourceName, item)) {
            return false;
        }

        // K-Startup은 팀 요구사항에 따라 유지하되,
        // 기업 행사/전시/해외진출성 공고를 줄이고 개인 창업지원 성격만 남긴다.
        if (containsAny(sourceName, KSTARTUP_SOURCES)
                && !isRelevantKStartup(item)) {
            return false;
        }

        // 기업마당 역시 소상공인·자영업·중장년 또는 서울 개인창업 지원 중심으로 제한한다.
        if (containsAny(sourceName, BIZINFO_SOURCES)
                && !isRelevantBizInfo(sourceName, item)) {
            return false;
        }

        if (!seoulOnly) {
            return true;
        }

        String explicitRegion = join(
                item.getRegion(),
                item.getDistrict()
        );

        if (containsAny(explicitRegion, SEOUL_WORDS)
                || containsAny(explicitRegion, SEOUL_DISTRICT_WORDS)) {
            return true;
        }

        if (includeNationwide
                && containsAny(explicitRegion, NATIONWIDE_WORDS)) {
            return true;
        }

        if (containsAny(explicitRegion, OTHER_REGIONS)) {
            return false;
        }

        if (containsAny(sourceName, SEOUL_BOUND_SOURCES)) {
            return true;
        }

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

        if (containsAny(fullText, SEOUL_WORDS)
                || containsAny(fullText, SEOUL_DISTRICT_WORDS)) {
            return true;
        }

        if (includeNationwide
                && containsAny(fullText, NATIONWIDE_WORDS)) {
            return true;
        }

        if (containsAny(fullText, OTHER_REGIONS)) {
            return false;
        }

        return includeNationwide
                && containsAny(sourceName, NATIONAL_DEFAULT_SOURCES);
    }

    public String resolveRegion(
            String sourceName,
            ExternalPolicyItem item
    ) {
        if (item == null) {
            return null;
        }

        String explicitRegion = join(
                item.getRegion(),
                item.getDistrict()
        );

        if (containsAny(explicitRegion, SEOUL_WORDS)
                || containsAny(explicitRegion, SEOUL_DISTRICT_WORDS)) {
            return "서울특별시";
        }

        if (includeNationwide
                && containsAny(explicitRegion, NATIONWIDE_WORDS)) {
            return "전국";
        }

        if (containsAny(explicitRegion, OTHER_REGIONS)) {
            return clean(item.getRegion());
        }

        if (containsAny(sourceName, SEOUL_BOUND_SOURCES)) {
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

        if (containsAny(fullText, SEOUL_WORDS)
                || containsAny(fullText, SEOUL_DISTRICT_WORDS)) {
            return "서울특별시";
        }

        if ((includeNationwide
                && containsAny(fullText, NATIONWIDE_WORDS))
                || containsAny(sourceName, NATIONAL_DEFAULT_SOURCES)) {
            return "전국";
        }

        return clean(item.getRegion());
    }

    private boolean isClearlyYouthOnly(ExternalPolicyItem item) {
        String title = item.getTitle();
        String target = item.getTarget();
        // 제목에 중장년 대상이 함께 명시된 경우에는 유지한다.
        boolean titleHasMiddleAge = containsAny(title, MIDAGE_WORDS);

        // 제목 자체가 명확하게 청년·청소년·학생 전용이면 제외한다.
        if (!titleHasMiddleAge
                && containsAny(title, YOUTH_ONLY_TITLE_WORDS)) {
            return true;
        }

        // 제목은 일반적이어도 대상이 만 34/39세 이하 등으로 명확히 제한되면 제외한다.
        // 단, 같은 대상 문구에 중장년/전 연령이 함께 명시되어 있으면 유지한다.
        boolean targetHasMiddleAge = containsAny(target, MIDAGE_WORDS);
        boolean targetIsAllAges = containsAny(
                target,
                List.of("전 연령", "전연령", "연령 제한 없음", "연령제한없음")
        );

        return !targetHasMiddleAge
                && !targetIsAllAges
                && containsAny(target, YOUTH_ONLY_TARGET_WORDS);
    }

    private boolean isAdvertisementLikeEducation(
            String sourceName,
            ExternalPolicyItem item
    ) {
        if (containsAny(sourceName, TRUSTED_PUBLIC_EDUCATION_SOURCES)) {
            return false;
        }

        String providerText = join(
                item.getAgencyName(),
                item.getContact(),
                item.getTitle()
        );

        String courseText = join(
                item.getTitle(),
                item.getSummary(),
                item.getBenefit(),
                item.getContentText(),
                item.getApplyMethod()
        );

        boolean privateEducationProvider =
                containsAny(providerText, AD_EDUCATION_PROVIDER_WORDS);

        boolean individualCourse =
                containsAny(courseText, AD_EDUCATION_COURSE_WORDS);

        return privateEducationProvider && individualCourse;
    }

    private boolean isRelevantKStartup(ExternalPolicyItem item) {
        String text = join(
                item.getTitle(),
                item.getAgencyName(),
                item.getSummary(),
                item.getTarget(),
                item.getRegion(),
                item.getDistrict(),
                item.getBenefit(),
                item.getContentText(),
                item.getContact()
        );

        // 전시회·데모데이·오픈이노베이션·해외진출 같은 B2B 행사성 공고는 제외한다.
        if (containsAny(text, BUSINESS_EVENT_WORDS)) {
            return false;
        }

        String locationText = join(
                item.getRegion(),
                item.getDistrict(),
                item.getTitle(),
                item.getAgencyName()
        );

        boolean seoulRelated = containsAny(locationText, SEOUL_WORDS)
                || containsAny(locationText, SEOUL_DISTRICT_WORDS);

        // 서울 관련 표기가 없는데 명백한 타지역명이 있으면 제외한다.
        if (!seoulRelated
                && containsAny(locationText, BUSINESS_NON_SEOUL_LOCATION_WORDS)) {
            return false;
        }

        String middleAgeText = join(
                item.getTitle(),
                item.getSummary(),
                item.getBenefit(),
                item.getContentText()
        );

        boolean middleAgeRelevant =
                containsAny(middleAgeText, MIDAGE_STRONG_WORDS);

        boolean personalStartupSupport =
                containsAny(text, KSTARTUP_PERSONAL_SUPPORT_WORDS);

        // K-Startup은 중장년 또는 개인/예비창업자·1인기업 지원 성격만 남긴다.
        return middleAgeRelevant || personalStartupSupport;
    }

    private boolean isRelevantBizInfo(
            String sourceName,
            ExternalPolicyItem item
    ) {
        String text = join(
                item.getTitle(),
                item.getAgencyName(),
                item.getSummary(),
                item.getTarget(),
                item.getRegion(),
                item.getDistrict(),
                item.getBenefit(),
                item.getSelectionCriteria(),
                item.getApplyMethod(),
                item.getContentText()
        );

        String locationText = join(
                item.getRegion(),
                item.getDistrict(),
                item.getTitle()
        );

        boolean seoulRelated = containsAny(locationText, SEOUL_WORDS)
                || containsAny(locationText, SEOUL_DISTRICT_WORDS);

        if (!seoulRelated
                && containsAny(locationText, BUSINESS_NON_SEOUL_LOCATION_WORDS)) {
            return false;
        }

        boolean coreRelevant = containsAny(text, BIZINFO_CORE_WORDS);
        boolean personalStartup = containsAny(text, BIZINFO_PERSONAL_STARTUP_WORDS);
        boolean businessEvent = containsAny(text, BUSINESS_EVENT_WORDS);

        // 행사정보는 서울 + 소상공인/자영업/중장년 등 핵심 대상일 때만 허용한다.
        if (sourceName != null && sourceName.contains("행사정보")) {
            return seoulRelated && coreRelevant;
        }

        // 일반 기업 대상 전시·데모데이·해외진출성 지원은 제외한다.
        if (businessEvent && !coreRelevant) {
            return false;
        }

        // 소상공인·자영업·중장년·재취업 관련은 전국 정책도 허용하고,
        // 일반 창업 지원은 서울 관련성이 있을 때만 허용한다.
        return coreRelevant || (seoulRelated && personalStartup);
    }

    private boolean containsAny(
            String value,
            List<String> keywords
    ) {
        String normalized = normalize(value);

        if (normalized == null) {
            return false;
        }

        return keywords.stream()
                .map(this::normalize)
                .filter(keyword -> keyword != null)
                .anyMatch(normalized::contains);
    }

    private String join(String... values) {
        StringBuilder result = new StringBuilder();

        for (String value : values) {
            String cleaned = clean(value);

            if (cleaned == null) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(cleaned);
        }

        return result.isEmpty()
                ? null
                : result.toString();
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        return cleaned.isBlank()
                ? null
                : cleaned;
    }

    private String normalize(String value) {
        String cleaned = clean(value);

        if (cleaned == null) {
            return null;
        }

        return cleaned
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "");
    }
}
