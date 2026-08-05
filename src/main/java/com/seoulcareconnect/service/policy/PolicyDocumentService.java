package com.seoulcareconnect.service.policy;

import com.seoulcareconnect.dto.policy.PolicyDocumentItemDTO;
import com.seoulcareconnect.entity.policy.DocumentGuide;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.PolicyDocument;
import com.seoulcareconnect.entity.policy.enums.DocumentCategory;
import com.seoulcareconnect.entity.policy.enums.DocumentRequirementType;
import com.seoulcareconnect.integration.policy.ExternalPolicyDocument;
import com.seoulcareconnect.repository.policy.PolicyDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyDocumentService {

    private static final int MAX_DOCUMENTS = 40;
    private static final Pattern LEADING_MARKER = Pattern.compile(
            "^\\s*(?:[•▪■◆◇▶▷※*\\-–—]+|\\d{1,2}[.)])\\s*"
    );
    private static final Pattern PARENTHETICAL = Pattern.compile("\\(([^)]{1,300})\\)");
    private static final List<String> NOISE_PHRASES = List.of(
            "중소벤처24 증명서 및 사업신청 통화서비스 제공",
            "중소기업현황정보시스템 중소기업확인서 신청 및 발급 제공",
            "개인정보처리방침",
            "이용약관",
            "이메일무단수집거부",
            "저작권정책"
    );
    private static final List<String> DOCUMENT_HINTS = List.of(
            "서류", "자료", "신청서", "계획서", "동의서", "서약서", "확인서",
            "증명", "등록증", "등본", "초본", "계약서", "견적서", "재무제표",
            "증빙", "사본", "이력서", "자기소개서", "포트폴리오", "자격증",
            "수료증", "상장", "발표", "ir", "소득", "납세", "과세", "보험",
            "연금", "재직", "경력", "매출", "학력", "실적", "추천", "신분",
            "통장", "계좌", "특허", "출원", "면허", "성적", "재학", "졸업",
            "수급", "장애", "가족", "혼인", "건축물", "토지", "부동산", "서명"
    );
    private static final List<String> CONDITION_HINTS = List.of(
            "경우", "해당", "필요시", "한하여", "이후 발급", "최근", "추후",
            "선수", "지도자", "심판", "사업자등록 이력"
    );

    private final PolicyDocumentRepository policyDocumentRepository;
    private final DocumentGuideService documentGuideService;

    @Transactional
    public void sync(
            Policy policy,
            String requiredDocumentsText,
            List<ExternalPolicyDocument> externalDocuments,
            String officialPageUrl
    ) {
        if (policy == null || policy.getPolicyId() == null) {
            return;
        }

        List<DocumentGuide> guides = documentGuideService.activeGuides();
        List<ExternalPolicyDocument> candidates = mergeCandidates(
                requiredDocumentsText,
                externalDocuments
        );
        List<PolicyDocument> documents = structure(
                policy,
                candidates,
                guides,
                safeHttpUrl(officialPageUrl)
        );

        policyDocumentRepository.deleteAllByPolicyId(policy.getPolicyId());
        if (!documents.isEmpty()) {
            policyDocumentRepository.saveAll(documents);
        }
    }

    @Transactional
    public int backfillMissingDocuments() {
        List<Policy> policies = policyDocumentRepository.findPoliciesNeedingDocumentBackfill();
        if (policies.isEmpty()) {
            return 0;
        }

        List<DocumentGuide> guides = documentGuideService.activeGuides();
        int count = 0;

        for (Policy policy : policies) {
            String text = policy.getDetail() == null
                    ? null
                    : policy.getDetail().getRequiredDocumentsText();
            List<PolicyDocument> documents = structure(
                    policy,
                    mergeCandidates(text, List.of()),
                    guides,
                    safeHttpUrl(policy.getOfficialUrl())
            );
            if (!documents.isEmpty()) {
                policyDocumentRepository.saveAll(documents);
                count++;
            }
        }

        log.info("기존 정책 필요서류 구조화 완료: {}건", count);
        return count;
    }

    @Transactional(readOnly = true)
    public List<PolicyDocumentItemDTO> findForPolicy(Long policyId) {
        return policyDocumentRepository
                .findByPolicy_PolicyIdOrderBySortOrderAscPolicyDocumentIdAsc(policyId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private List<ExternalPolicyDocument> mergeCandidates(
            String requiredDocumentsText,
            List<ExternalPolicyDocument> externalDocuments
    ) {
        Map<String, ExternalPolicyDocument> merged = new LinkedHashMap<>();

        if (externalDocuments != null) {
            for (ExternalPolicyDocument document : externalDocuments) {
                if (document == null) {
                    continue;
                }
                String text = clean(document.getText());
                if (!isUseful(text)) {
                    continue;
                }
                merged.putIfAbsent(compact(text), document);
            }
        }

        for (String text : splitRawText(requiredDocumentsText)) {
            String key = compact(text);
            merged.putIfAbsent(
                    key,
                    ExternalPolicyDocument.builder()
                            .text(text)
                            .sourceType("STORED_TEXT")
                            .confidence(65)
                            .build()
            );
        }

        return merged.values().stream().limit(MAX_DOCUMENTS).toList();
    }

    private List<PolicyDocument> structure(
            Policy policy,
            List<ExternalPolicyDocument> candidates,
            List<DocumentGuide> guides,
            String officialPageUrl
    ) {
        List<PolicyDocument> result = new ArrayList<>();
        Set<String> usedChecklistKeys = new LinkedHashSet<>();
        int order = 0;

        for (ExternalPolicyDocument candidate : candidates) {
            String original = clean(candidate.getText());
            if (!isUseful(original)) {
                continue;
            }

            List<GuideMatch> matches = findGuideMatches(original, guides);
            if (matches.isEmpty()) {
                PolicyDocument document = createDocument(
                        policy,
                        candidate,
                        original,
                        null,
                        officialPageUrl,
                        order
                );
                if (usedChecklistKeys.add(document.getChecklistKey())) {
                    result.add(document);
                    order++;
                }
                continue;
            }

            for (GuideMatch match : matches) {
                PolicyDocument document = createDocument(
                        policy,
                        candidate,
                        original,
                        match.guide(),
                        officialPageUrl,
                        order
                );
                if (usedChecklistKeys.add(document.getChecklistKey())) {
                    result.add(document);
                    order++;
                }
            }
        }

        return result.stream().limit(MAX_DOCUMENTS).toList();
    }

    private PolicyDocument createDocument(
            Policy policy,
            ExternalPolicyDocument candidate,
            String original,
            DocumentGuide guide,
            String officialPageUrl,
            int order
    ) {
        DocumentRequirementType requirementType = requirementType(original);
        String normalizedKey = guide == null
                ? truncate(compact(original), 180)
                : guide.getGuideKey();

        PolicyDocument document = new PolicyDocument();
        document.setPolicy(policy);
        document.setGuide(guide);
        document.setOriginalText(original);
        document.setDisplayName(guide == null ? truncate(original, 300) : guide.getDisplayName());
        document.setNormalizedKey(normalizedKey);
        document.setChecklistKey(truncate(normalizedKey, 220));
        document.setCategory(guide == null ? inferCategory(original) : guide.getCategory());
        document.setRequirementType(requirementType);
        document.setConditionText(extractCondition(original));
        document.setAlternativeGroup(
                requirementType == DocumentRequirementType.ALTERNATIVE
                        ? "ALT-" + Integer.toUnsignedString(compact(original).hashCode(), 36)
                        : null
        );
        document.setSourceType(truncate(clean(candidate.getSourceType()), 40));
        document.setAttachmentName(truncate(clean(candidate.getAttachmentName()), 500));
        document.setDownloadUrl(safeHttpUrl(candidate.getDownloadUrl()));
        document.setOfficialPageUrl(officialPageUrl);
        document.setConfidence(clamp(candidate.getConfidence()));
        document.setSortOrder(order);
        return document;
    }

    private List<GuideMatch> findGuideMatches(
            String original,
            List<DocumentGuide> guides
    ) {
        String compactOriginal = compact(original);
        List<GuideMatch> matches = new ArrayList<>();

        for (DocumentGuide guide : guides) {
            for (String alias : splitLines(guide.getAliasesText())) {
                String compactAlias = compact(alias);
                if (compactAlias.length() < 3) {
                    continue;
                }

                int start = compactOriginal.indexOf(compactAlias);
                while (start >= 0) {
                    matches.add(new GuideMatch(guide, compactAlias, start));
                    start = compactOriginal.indexOf(compactAlias, start + 1);
                }
            }
        }

        matches.sort(Comparator.comparingInt(
                (GuideMatch match) -> match.alias().length()
        ).reversed());

        List<GuideMatch> selected = new ArrayList<>();
        for (GuideMatch match : matches) {
            boolean shadowed = selected.stream().anyMatch(selectedMatch ->
                    match.start() >= selectedMatch.start()
                            && match.end() <= selectedMatch.end()
            );
            if (!shadowed) {
                selected.add(match);
            }
        }

        Map<String, GuideMatch> uniqueGuides = new LinkedHashMap<>();
        selected.stream()
                .sorted(Comparator.comparingInt(GuideMatch::start))
                .forEach(match -> uniqueGuides.putIfAbsent(
                        match.guide().getGuideKey(),
                        match
                ));
        return new ArrayList<>(uniqueGuides.values());
    }

    private List<String> splitRawText(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (String part : value.split("(?:\\r?\\n)+|[•▪■◆◇▶▷]+|;")) {
            String cleaned = clean(part);
            if (isUseful(cleaned)) {
                result.add(cleaned);
            }
        }
        return result;
    }

    private boolean isUseful(String value) {
        if (!StringUtils.hasText(value) || value.length() > 600) {
            return false;
        }

        String compactValue = compact(value);
        if (NOISE_PHRASES.stream().map(this::compact).anyMatch(compactValue::contains)
                || compactValue.endsWith(compact("서비스 제공"))
                || compactValue.endsWith(compact("신청 및 발급 제공"))) {
            return false;
        }

        boolean documentHint = DOCUMENT_HINTS.stream()
                .map(this::compact)
                .anyMatch(compactValue::contains);
        if (documentHint) {
            return true;
        }

        return value.length() <= 180
                && !containsAny(
                compactValue,
                "바랍니다",
                "해주세요",
                "확인하세요",
                "진행됩니다",
                "제공합니다",
                "문의하세요",
                "참고하세요"
        );
    }

    private DocumentRequirementType requirementType(String value) {
        String compactValue = compact(value);
        if (containsAny(compactValue, "또는", "택1", "중하나", "대체서류")) {
            return DocumentRequirementType.ALTERNATIVE;
        }
        if (containsAny(compactValue, "필요시", "필요한경우", "선택", "추후안내")) {
            return DocumentRequirementType.OPTIONAL;
        }
        if (containsAny(compactValue, "해당자", "해당하는경우", "인경우", "경우에만",
                "한하여", "사업자등록이력이", "선수:", "지도자", "심판")) {
            return DocumentRequirementType.CONDITIONAL;
        }
        return DocumentRequirementType.REQUIRED;
    }

    private String extractCondition(String value) {
        List<String> conditions = new ArrayList<>();
        Matcher matcher = PARENTHETICAL.matcher(value);

        while (matcher.find()) {
            String content = clean(matcher.group(1));
            if (content != null && CONDITION_HINTS.stream().anyMatch(content::contains)) {
                conditions.add(content);
            }
        }

        int colon = value.indexOf(':');
        if (colon > 0 && colon <= 100) {
            String prefix = clean(value.substring(0, colon));
            if (prefix != null && CONDITION_HINTS.stream().anyMatch(prefix::contains)) {
                conditions.add(prefix);
            }
        }

        if (conditions.isEmpty()) {
            return null;
        }
        return truncate(String.join(" · ", new LinkedHashSet<>(conditions)), 500);
    }

    private DocumentCategory inferCategory(String value) {
        String compactValue = compact(value);
        if (containsAny(compactValue, "주민", "가족", "혼인", "신분증")) {
            return DocumentCategory.RESIDENT_FAMILY;
        }
        if (containsAny(compactValue, "소득", "세금", "납세", "부가가치세", "원천징수")) {
            return DocumentCategory.INCOME_TAX;
        }
        if (containsAny(compactValue, "건강보험", "국민연금", "고용보험")) {
            return DocumentCategory.SOCIAL_INSURANCE;
        }
        if (containsAny(compactValue, "재직", "경력")) {
            return DocumentCategory.EMPLOYMENT_CAREER;
        }
        if (containsAny(compactValue, "사업자", "법인", "재무", "매출")) {
            return DocumentCategory.BUSINESS_CORPORATE;
        }
        if (containsAny(compactValue, "임대차", "건축물", "토지", "부동산")) {
            return DocumentCategory.HOUSING_PROPERTY;
        }
        if (containsAny(compactValue, "졸업", "재학", "성적", "자격", "수료")) {
            return DocumentCategory.EDUCATION_QUALIFICATION;
        }
        if (containsAny(compactValue, "장애", "수급", "한부모")) {
            return DocumentCategory.WELFARE_ELIGIBILITY;
        }
        if (containsAny(compactValue, "통장", "계좌")) {
            return DocumentCategory.FINANCE_PAYMENT;
        }
        if (containsAny(compactValue, "신청서", "동의서", "서약서", "별첨", "서식")) {
            return DocumentCategory.ANNOUNCEMENT_FORM;
        }
        if (containsAny(compactValue, "계획서", "발표", "포트폴리오", "이력서", "자기소개")) {
            return DocumentCategory.SELF_WRITTEN;
        }
        if (containsAny(compactValue, "증빙", "자료")) {
            return DocumentCategory.GENERAL_EVIDENCE;
        }
        return DocumentCategory.OTHER;
    }

    private PolicyDocumentItemDTO toDto(PolicyDocument document) {
        DocumentGuide guide = document.getGuide();
        return PolicyDocumentItemDTO.builder()
                .policyDocumentId(document.getPolicyDocumentId())
                .checklistKey(document.getChecklistKey())
                .originalText(document.getOriginalText())
                .displayName(document.getDisplayName())
                .category(document.getCategory())
                .categoryLabel(document.getCategory().getLabel())
                .requirementType(document.getRequirementType())
                .requirementLabel(document.getRequirementType().getLabel())
                .conditionText(document.getConditionText())
                .alternativeGroup(document.getAlternativeGroup())
                .attachmentName(document.getAttachmentName())
                .downloadUrl(document.getDownloadUrl())
                .officialPageUrl(document.getOfficialPageUrl())
                .confidence(document.getConfidence())
                .guideAvailable(guide != null)
                .guideTitle(guide == null ? document.getDisplayName() : guide.getDisplayName())
                .issuer(guide == null ? null : guide.getIssuer())
                .officialGuideUrl(guide == null ? null : safeHttpUrl(guide.getOfficialUrl()))
                .helpName(guide == null ? null : guide.getHelpName())
                .helpUrl(guide == null ? null : safeHttpUrl(guide.getHelpUrl()))
                .guideSummary(guide == null ? null : guide.getSummary())
                .steps(guide == null ? List.of() : splitLines(guide.getStepsText()))
                .preparationItems(
                        guide == null ? List.of() : splitLines(guide.getPreparationText())
                )
                .caution(guide == null ? null : guide.getCaution())
                .verifiedOn(guide == null ? null : guide.getVerifiedOn())
                .build();
    }

    private List<String> splitLines(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return value.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(compact(candidate))) {
                return true;
            }
        }
        return false;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        cleaned = LEADING_MARKER.matcher(cleaned).replaceFirst("");
        cleaned = cleaned.replaceFirst(
                "^(?:제출서류|제출 서류|필요서류|필요 서류|구비서류|구비 서류)\\s*[:：-]?\\s*",
                ""
        ).trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private String compact(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT).replaceAll("[^0-9a-z가-힣]", "");
    }

    private String safeHttpUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            URI uri = URI.create(value.trim());
            if (!"http".equalsIgnoreCase(uri.getScheme())
                    && !"https".equalsIgnoreCase(uri.getScheme())) {
                return null;
            }
            return uri.getHost() == null ? null : truncate(uri.toString(), 1000);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private int clamp(Integer value) {
        if (value == null) {
            return 50;
        }
        return Math.max(0, Math.min(100, value));
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record GuideMatch(DocumentGuide guide, String alias, int start) {
        int end() {
            return start + alias.length();
        }
    }
}
