package com.seoulcareconnect.integration.policy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.integration.policy.ExternalApiSupport;
import com.seoulcareconnect.integration.policy.ExternalDateParser;
import com.seoulcareconnect.integration.policy.ExternalNodeReader;
import com.seoulcareconnect.integration.policy.ExternalPolicyClassifier;
import com.seoulcareconnect.integration.policy.ExternalPolicyClient;
import com.seoulcareconnect.integration.policy.ExternalPolicyItem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

abstract class AbstractMyHomeClient implements ExternalPolicyClient {

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final ExternalApiSupport api;
    private final ExternalNodeReader reader;
    private final ExternalDateParser dateParser;
    private final ExternalPolicyClassifier classifier;
    private final String serviceKey;
    private final String url;
    private final String regionCode;
    private final int pageNo;
    private final int numOfRows;
    private final int maxPages;

    protected AbstractMyHomeClient(
            ExternalApiSupport api,
            ExternalNodeReader reader,
            ExternalDateParser dateParser,
            ExternalPolicyClassifier classifier,
            String serviceKey,
            String url,
            String regionCode,
            int pageNo,
            int numOfRows,
            int maxPages
    ) {
        this.api = api;
        this.reader = reader;
        this.dateParser = dateParser;
        this.classifier = classifier;
        this.serviceKey = serviceKey;
        this.url = url;
        this.regionCode = regionCode;
        this.pageNo = Math.max(pageNo, 1);
        this.numOfRows = Math.min(Math.max(numOfRows, 1), 100);
        this.maxPages = Math.max(maxPages, 1);
    }

    @Override
    public String sourceBaseUrl() {
        return url;
    }

    @Override
    public String sourceCategory() {
        return "주거";
    }

    @Override
    public List<ExternalPolicyItem> fetch() {
        /*
         * 마이홈 API는 같은 공고(pblancId)를 단지별 houseSn으로 여러 번 내려줄 수 있습니다.
         * 같은 공고를 여러 정책으로 만들지 않고 pblancId 기준으로 묶어서 한 정책으로 저장합니다.
         */
        Map<String, GroupedItems> groupedItems = new LinkedHashMap<>();
        int fallbackSequence = 0;

        for (int page = pageNo; page < pageNo + maxPages; page++) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("serviceKey", serviceKey);
            params.put("brtcCode", regionCode);
            params.put("pageNo", page);
            params.put("numOfRows", numOfRows);
            params.put("_type", "json");

            String raw = api.get(url, params);
            JsonNode root = api.readJson(raw);

            List<JsonNode> items = reader.findObjectsContainingAny(
                    root,
                    "rcritPblancNm",
                    "pblancNm",
                    "panNm",
                    "hsmpNm"
            );

            if (items.isEmpty()) {
                break;
            }

            for (JsonNode item : items) {
                String groupKey = groupKey(item, fallbackSequence++);

                GroupedItems group = groupedItems.computeIfAbsent(
                        groupKey,
                        ignored -> new GroupedItems(raw)
                );

                group.items.add(item);
            }

            if (items.size() < numOfRows) {
                break;
            }
        }

        List<ExternalPolicyItem> result = new ArrayList<>();

        for (GroupedItems group : groupedItems.values()) {
            result.add(mapGroup(group));
        }

        return result;
    }

    private String groupKey(JsonNode item, int fallbackSequence) {
        String announcementId = reader.firstText(
                item,
                "rcritNtcId",
                "pblancId",
                "panId"
        );

        if (hasText(announcementId)) {
            return announcementId;
        }

        String title = reader.firstText(
                item,
                "rcritPblancNm",
                "pblancNm",
                "panNm",
                "hsmpNm",
                "title"
        );

        String houseNumber = reader.firstText(
                item,
                "houseSn",
                "hsmpSn",
                "seq"
        );

        return reader.joinNonBlank(
                "-",
                title,
                houseNumber,
                String.valueOf(fallbackSequence)
        );
    }

    private ExternalPolicyItem mapGroup(GroupedItems group) {
        List<JsonNode> items = group.items;

        String externalId = firstText(
                items,
                "rcritNtcId",
                "pblancId",
                "panId"
        );

        String title = firstText(
                items,
                "rcritPblancNm",
                "pblancNm",
                "panNm",
                "hsmpNm",
                "title"
        );

        String agencyName = firstText(
                items,
                "suplyInsttNm",
                "insttNm",
                "agency"
        );

        String houseType = firstText(
                items,
                "houseTyNm",
                "houseType"
        );

        String supplyType = firstText(
                items,
                "suplyTyNm",
                "supplyType"
        );

        /*
         * houseTyNm(예: 아파트)은 지원 대상이 아니므로 target으로 사용하지 않습니다.
         * API가 실제 대상 필드를 주면 우선 사용하고, 없으면 공고명에서 안전한 수준으로 추출합니다.
         */
        String explicitTarget = firstText(
                items,
                "suplyTrget",
                "trgetNm",
                "target"
        );

        String target = hasText(explicitTarget)
                ? explicitTarget
                : resolveTarget(title, supplyType);

        String region = reader.joinNonBlank(
                " ",
                firstText(items, "brtcNm", "sidoNm"),
                firstText(items, "signguNm", "sggNm")
        );

        if (!hasText(region)) {
            region = "서울특별시";
        }

        String period = firstText(
                items,
                "rcritBeginEndDe",
                "reqstBeginEndDe",
                "rceptPd"
        );

        ExternalDateParser.DateRange range = dateParser.parseRange(period);

        LocalDate startDate = range.startDate() != null
                ? range.startDate()
                : earliestDate(
                items,
                "rcritBgngDe",
                "beginDe",
                "startDate"
        );

        LocalDate endDate = range.endDate() != null
                ? range.endDate()
                : latestDate(
                items,
                "rcritEndDe",
                "endDe",
                "endDate"
        );

        String statusText = firstText(
                items,
                "sttusNm",
                "rcritSttusNm",
                "status",
                "rcrt_prgs_yn"
        );

        String officialUrl = firstText(
                items,
                "url",
                "pcUrl",
                "mobileUrl",
                "pblancUrl",
                "dtlUrl",
                "link"
        );

        String directApplyMethod = firstText(
                items,
                "reqstMth",
                "aplyMthd",
                "applyMethod"
        );

        String applyMethod = hasText(directApplyMethod)
                ? directApplyMethod
                : resolveApplyMethod(officialUrl);

        String reference = firstText(
                items,
                "refrnc",
                "inqireTel",
                "telNo",
                "contact"
        );

        String supplyCount = totalSupplyCount(items);
        String complexNames = distinctJoined(items, "hsmpNm");

        String summary = buildSummary(
                agencyName,
                supplyType,
                region,
                supplyCount,
                target
        );

        String benefit = reader.joinNonBlank(
                "\n",
                labeled("공급 유형", supplyType),
                labeled("주택 유형", houseType),
                labeled("공급 규모", supplyCount),
                labeled("공급 기관", agencyName),
                labeled("대상 지역", region),
                labeled("단지", complexNames)
        );

        String contentText = buildContentText(
                items,
                title,
                agencyName,
                houseType,
                supplyType,
                supplyCount,
                region,
                statusText,
                reference
        );

        String selectionCriteria = firstText(
                items,
                "slctnStdr",
                "selectionCriteria"
        );

        if (!hasText(selectionCriteria)) {
            selectionCriteria =
                    "세부 입주 자격과 선정 기준은 공식 모집공고문을 확인해 주세요.";
        }

        String requiredDocuments = firstText(
                items,
                "reqrdDoc",
                "requiredDocuments",
                "requiredDocumentsText"
        );

        if (!hasText(requiredDocuments)) {
            requiredDocuments =
                    "제출 서류는 신청 유형과 자격에 따라 달라질 수 있으므로 공식 모집공고문을 확인해 주세요.";
        }

        return ExternalPolicyItem.builder()
                .sourceName(sourceName())
                .sourceBaseUrl(sourceBaseUrl())
                .externalId(externalId)
                .title(reader.stripHtml(title))
                .agencyName(reader.stripHtml(agencyName))
                .summary(reader.stripHtml(summary))
                .category(PolicyCategory.HOUSING)
                .target(reader.stripHtml(target))
                .region(reader.stripHtml(region))
                .district(classifier.district(region, contentText))
                .startDate(startDate)
                .endDate(endDate)
                .statusText(reader.stripHtml(statusText))
                .applyMethod(reader.stripHtml(applyMethod))
                .officialUrl(officialUrl)
                .contact(reader.stripHtml(reference))
                .benefit(reader.stripHtml(benefit))
                .selectionCriteria(reader.stripHtml(selectionCriteria))
                .requiredDocumentsText(reader.stripHtml(requiredDocuments))
                .contentText(reader.stripHtml(contentText))
                .rawJson(group.rawJson)
                .httpStatus(200)
                .build();
    }

    private String buildSummary(
            String agencyName,
            String supplyType,
            String region,
            String supplyCount,
            String target
    ) {
        String intro;

        if (hasText(agencyName) && hasText(supplyType)) {
            intro = agencyName + "에서 공급하는 " + supplyType + " 모집공고입니다.";
        } else if (hasText(supplyType)) {
            intro = supplyType + " 모집공고입니다.";
        } else {
            intro = "주택 입주자 모집공고입니다.";
        }

        return reader.joinNonBlank(
                " ",
                intro,
                hasText(target) ? "주요 대상: " + target + "." : null,
                hasText(region) ? "지역: " + region + "." : null,
                hasText(supplyCount) ? "공급 규모: " + supplyCount + "." : null
        );
    }

    private String buildContentText(
            List<JsonNode> items,
            String title,
            String agencyName,
            String houseType,
            String supplyType,
            String supplyCount,
            String region,
            String statusText,
            String reference
    ) {
        StringBuilder content = new StringBuilder();

        appendLine(content, "공고명", title);
        appendLine(content, "공고 유형", statusText);
        appendLine(content, "공급 기관", agencyName);
        appendLine(content, "공급 유형", supplyType);
        appendLine(content, "주택 유형", houseType);
        appendLine(content, "공급 규모", supplyCount);
        appendLine(content, "대상 지역", region);

        appendLine(
                content,
                "모집 공고일",
                formattedDate(
                        firstText(
                                items,
                                "rcritPblancDe",
                                "pblancDe"
                        )
                )
        );

        appendLine(
                content,
                "당첨자 발표일",
                formattedDate(
                        firstText(
                                items,
                                "przwnerPresnatnDe",
                                "resultDate"
                        )
                )
        );

        Set<String> complexDetails = new LinkedHashSet<>();

        for (JsonNode item : items) {
            String detail = buildComplexDetail(item);

            if (hasText(detail)) {
                complexDetails.add(detail);
            }
        }

        if (!complexDetails.isEmpty()) {
            if (content.length() > 0) {
                content.append("\n");
            }

            content.append("단지별 정보");

            for (String detail : complexDetails) {
                content.append("\n- ").append(detail);
            }
        }

        appendLine(content, "문의처", reference);

        return content.length() == 0
                ? null
                : content.toString();
    }

    private String buildComplexDetail(JsonNode item) {
        String complexName = reader.firstText(item, "hsmpNm");

        String houseNumber = reader.firstText(
                item,
                "houseSn",
                "hsmpSn"
        );

        String displayName = complexName;

        if (!hasText(displayName)
                && hasText(houseNumber)
                && !"0".equals(houseNumber)) {
            displayName = "단지 " + houseNumber;
        }

        String supplyCount = positiveCount(
                reader.firstText(
                        item,
                        "sumSuplyCo",
                        "suplyHoCo"
                )
        );

        String address = reader.firstText(
                item,
                "fullAdres",
                "adres",
                "address"
        );

        String heatMethod = reader.firstText(item, "heatMthdNm");

        return reader.joinNonBlank(
                " / ",
                labeled("단지", displayName),
                labeled("공급", supplyCount),
                labeled("주소", address),
                labeled("난방", heatMethod),
                labeled("임대보증금", money(reader.firstText(item, "rentGtn"))),
                labeled("계약금", money(reader.firstText(item, "enty"))),
                labeled("중도금", money(reader.firstText(item, "prtpay"))),
                labeled("잔금", money(reader.firstText(item, "surlus"))),
                labeled("월 임대료", money(reader.firstText(item, "mtRntchrg")))
        );
    }

    private String resolveTarget(String title, String supplyType) {
        String normalizedTitle = title == null
                ? ""
                : title.replaceAll("\\s+", "");

        if (normalizedTitle.contains("다자녀")) {
            return "다자녀 가구";
        }

        if (normalizedTitle.contains("신혼")
                && normalizedTitle.contains("신생아")) {
            return "신혼부부·신생아 가구";
        }

        if (normalizedTitle.contains("신혼")) {
            return "신혼부부";
        }

        if (normalizedTitle.contains("신생아")) {
            return "신생아 가구";
        }

        if (normalizedTitle.contains("대학생")) {
            return "대학생";
        }

        if (normalizedTitle.contains("청년")) {
            return "청년";
        }

        if (normalizedTitle.contains("고령자")
                || normalizedTitle.contains("어르신")) {
            return "고령자";
        }

        if (normalizedTitle.contains("주거급여")) {
            return "주거급여 수급자";
        }

        if (normalizedTitle.contains("예비입주자")
                && hasText(supplyType)) {
            return supplyType + " 예비입주 신청자";
        }

        if (hasText(supplyType)) {
            return supplyType + " 입주 신청자";
        }

        return "공식 공고의 입주 자격 대상자";
    }

    private String resolveApplyMethod(String officialUrl) {
        if (!hasText(officialUrl)) {
            return null;
        }

        String normalized = officialUrl.toLowerCase(Locale.ROOT);

        if (normalized.contains("apply.lh.or.kr")) {
            return "LH 청약플러스에서 온라인 신청";
        }

        return "공식 공고에서 신청 방법 확인";
    }

    private LocalDate earliestDate(
            List<JsonNode> items,
            String... fieldNames
    ) {
        LocalDate result = null;

        for (JsonNode item : items) {
            LocalDate parsed = dateParser.parseSingle(
                    reader.firstText(item, fieldNames)
            );

            if (parsed != null
                    && (result == null || parsed.isBefore(result))) {
                result = parsed;
            }
        }

        return result;
    }

    private LocalDate latestDate(
            List<JsonNode> items,
            String... fieldNames
    ) {
        LocalDate result = null;

        for (JsonNode item : items) {
            LocalDate parsed = dateParser.parseSingle(
                    reader.firstText(item, fieldNames)
            );

            if (parsed != null
                    && (result == null || parsed.isAfter(result))) {
                result = parsed;
            }
        }

        return result;
    }

    private String firstText(
            List<JsonNode> items,
            String... fieldNames
    ) {
        for (JsonNode item : items) {
            String value = reader.firstText(item, fieldNames);

            if (hasText(value)) {
                return value;
            }
        }

        return null;
    }

    private String totalSupplyCount(List<JsonNode> items) {
        String explicitCount = firstMeaningfulCount(items, "suplyHoCo");

        if (hasText(explicitCount)) {
            return explicitCount;
        }

        long total = 0L;

        for (JsonNode item : items) {
            Long count = positiveLong(
                    reader.firstText(item, "sumSuplyCo")
            );

            if (count != null) {
                total += count;
            }
        }

        return total > 0
                ? String.format(Locale.KOREA, "%,d호", total)
                : null;
    }

    private String firstMeaningfulCount(
            List<JsonNode> items,
            String... fieldNames
    ) {
        for (JsonNode item : items) {
            String value = reader.firstText(item, fieldNames);

            if (!hasText(value) || "0".equals(value.trim())) {
                continue;
            }

            return value;
        }

        return null;
    }

    private String distinctJoined(
            List<JsonNode> items,
            String... fieldNames
    ) {
        Set<String> values = new LinkedHashSet<>();

        for (JsonNode item : items) {
            String value = reader.firstText(item, fieldNames);

            if (hasText(value)) {
                values.add(value);
            }
        }

        return values.isEmpty()
                ? null
                : String.join(", ", values);
    }

    private String positiveCount(String value) {
        Long parsed = positiveLong(value);

        return parsed == null
                ? null
                : String.format(Locale.KOREA, "%,d호", parsed);
    }

    private String money(String value) {
        Long parsed = positiveLong(value);

        return parsed == null
                ? null
                : String.format(Locale.KOREA, "%,d원", parsed);
    }

    private Long positiveLong(String value) {
        if (!hasText(value)) {
            return null;
        }

        String number = value.replaceAll("[^0-9-]", "");

        if (number.isBlank()) {
            return null;
        }

        try {
            long parsed = Long.parseLong(number);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String formattedDate(String value) {
        if (!hasText(value)) {
            return null;
        }

        LocalDate parsed = dateParser.parseSingle(value);

        return parsed == null
                ? value
                : parsed.format(DISPLAY_DATE);
    }

    private String labeled(String label, String value) {
        return hasText(value)
                ? label + ": " + value
                : null;
    }

    private void appendLine(
            StringBuilder builder,
            String label,
            String value
    ) {
        if (!hasText(value)) {
            return;
        }

        if (builder.length() > 0) {
            builder.append("\n");
        }

        builder
                .append(label)
                .append(": ")
                .append(value);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static final class GroupedItems {

        private final String rawJson;
        private final List<JsonNode> items = new ArrayList<>();

        private GroupedItems(String rawJson) {
            this.rawJson = rawJson;
        }
    }
}