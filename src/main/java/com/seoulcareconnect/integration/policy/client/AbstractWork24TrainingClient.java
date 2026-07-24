package com.seoulcareconnect.integration.policy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.integration.policy.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

abstract class AbstractWork24TrainingClient implements ExternalPolicyClient {

    private final ExternalApiSupport api;
    private final ExternalNodeReader reader;
    private final ExternalDateParser dateParser;
    private final ExternalPolicyClassifier classifier;
    private final String authKey;
    private final String returnType;
    private final String url;
    private final String outType;
    private final int pageNum;
    private final int pageSize;
    private final String areaCode;
    private final String sort;
    private final String sortColumn;
    private final int maxPages;
    private final int lookbackDays;
    private final int futureMonths;
    private final String zoneId;

    protected AbstractWork24TrainingClient(
            ExternalApiSupport api,
            ExternalNodeReader reader,
            ExternalDateParser dateParser,
            ExternalPolicyClassifier classifier,
            String authKey, String returnType, String url, String outType,
            int pageNum, int pageSize, String areaCode,
            String sort, String sortColumn, int maxPages,
            int lookbackDays, int futureMonths, String zoneId
    ) {
        this.api = api;
        this.reader = reader;
        this.dateParser = dateParser;
        this.classifier = classifier;
        this.authKey = authKey;
        this.returnType = returnType;
        this.url = url;
        this.outType = outType;
        this.pageNum = Math.max(pageNum, 1);
        this.pageSize = Math.min(Math.max(pageSize, 1), 100);
        this.areaCode = areaCode;
        this.sort = sort;
        this.sortColumn = sortColumn;
        this.maxPages = Math.max(maxPages, 1);
        this.lookbackDays = Math.max(lookbackDays, 0);
        this.futureMonths = Math.max(futureMonths, 1);
        this.zoneId = zoneId;
    }

    @Override public String sourceBaseUrl() { return url; }
    @Override public String sourceCategory() { return "교육"; }

    @Override
    public List<ExternalPolicyItem> fetch() {
        LocalDate today = LocalDate.now(ZoneId.of(zoneId));
        String searchStart = today.minusDays(lookbackDays).format(DateTimeFormatter.BASIC_ISO_DATE);
        String searchEnd = today.plusMonths(futureMonths).format(DateTimeFormatter.BASIC_ISO_DATE);
        List<ExternalPolicyItem> result = new ArrayList<>();

        for (int page = pageNum; page < pageNum + maxPages; page++) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("authKey", authKey);
            params.put("returnType", returnType);
            params.put("outType", outType);
            params.put("pageNum", page);
            params.put("pageSize", pageSize);
            params.put("srchTraStDt", searchStart);
            params.put("srchTraEndDt", searchEnd);
            params.put("srchTraArea1", areaCode);
            params.put("sort", sort);
            params.put("sortCol", sortColumn);

            String raw = api.get(url, params);
            JsonNode root = api.readXml(raw);
            List<JsonNode> items = reader.findObjectsContainingAny(root, "title").stream()
                    .filter(item -> reader.firstText(item, "trprId", "TRPR_ID") != null)
                    .toList();

            if (items.isEmpty()) break;
            items.forEach(item -> result.add(mapItem(item, raw)));
            if (items.size() < pageSize) break;
        }
        return result;
    }

    private ExternalPolicyItem mapItem(JsonNode item, String rawXml) {
        String title = reader.firstText(item, "title", "TITLE");
        String agencyName = reader.firstText(item, "subTitle", "SUB_TITLE");
        String summary = reader.firstText(item, "contents", "CONTENTS");
        String address = reader.firstText(item, "address", "ADDRESS");
        String target = reader.firstText(item, "trainTarget", "TRAIN_TARGET");
        String id = reader.firstText(item, "trprId", "TRPR_ID");
        String degree = reader.firstText(item, "trprDegr", "TRPR_DEGR");

        String cost = buildTrainingCost(item);

        String benefit = reader.joinNonBlank(
                "\n",
                summary,
                cost
        );

        return ExternalPolicyItem.builder()
                .sourceName(sourceName()).sourceBaseUrl(sourceBaseUrl())
                .externalId(id == null ? null : id + "-" + (degree == null ? "0" : degree))
                .title(reader.stripHtml(title))
                .agencyName(reader.stripHtml(agencyName))
                .summary(reader.stripHtml(summary))
                .category(PolicyCategory.EDUCATION)
                .target(reader.stripHtml(target))
                .region("서울특별시")
                .district(classifier.district(address))
                .startDate(dateParser.parseSingle(reader.firstText(item, "traStartDate", "TRA_START_DATE")))
                .endDate(dateParser.parseSingle(reader.firstText(item, "traEndDate", "TRA_END_DATE")))
                .applicationInfoAvailable(false)
                .applyMethod("고용24 공식 페이지에서 신청")
                .officialUrl(reader.firstText(item, "titleLink", "TITLE_LINK", "subTitleLink", "SUB_TITLE_LINK"))
                .contact(reader.stripHtml(
                        reader.firstText(item, "telNo", "TEL_NO")
                ))
                .benefit(reader.stripHtml(benefit))
                .contentText(reader.stripHtml(reader.joinNonBlank("\n", benefit, address, target)))
                .rawXml(rawXml).httpStatus(200)
                .build();
    }

    private String buildTrainingCost(JsonNode item) {
        String courseFee = formatMoney(
                reader.firstText(
                        item,
                        "courseMan",
                        "COURSE_MAN"
                )
        );

        String actualFee = formatMoney(
                reader.firstText(
                        item,
                        "realMan",
                        "REAL_MAN"
                )
        );

        if (isZeroAmount(courseFee) && actualFee != null) {
            return "훈련비: " + actualFee;
        }

        if (isZeroAmount(actualFee) && courseFee != null) {
            return "훈련비: " + courseFee;
        }

        if (courseFee == null) {
            return actualFee == null
                    ? null
                    : "훈련비: " + actualFee;
        }

        if (actualFee == null || courseFee.equals(actualFee)) {
            return "훈련비: " + courseFee;
        }

        return reader.joinNonBlank(
                "\n",
                "수강비: " + courseFee,
                "실제 훈련비: " + actualFee
        );
    }

    private String formatMoney(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String digits = value.replaceAll("[^0-9]", "");

        if (digits.isBlank()) {
            return value.trim();
        }

        try {
            long amount = Long.parseLong(digits);

            return String.format(
                    Locale.KOREA,
                    "%,d원",
                    amount
            );
        } catch (NumberFormatException ignored) {
            return value.trim();
        }
    }

    private boolean isZeroAmount(String value) {
        return value != null
                && value.replaceAll("[^0-9]", "")
                .matches("0+");
    }

}