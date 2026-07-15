package com.seoulcareconnect.integration.policy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.integration.policy.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.external.fifty-plus.enabled", havingValue = "true")
public class FiftyPlusEducationClient implements ExternalPolicyClient {

    private final ExternalApiSupport api;
    private final ExternalNodeReader reader;
    private final ExternalDateParser dateParser;
    private final String baseUrl;
    private final String apiKey;
    private final String responseType;
    private final int pageNo;
    private final int numOfRows;
    private final int maxPages;
    private final String zoneId;

    public FiftyPlusEducationClient(
            ExternalApiSupport api,
            ExternalNodeReader reader,
            ExternalDateParser dateParser,
            @Value("${app.external.fifty-plus.base-url}") String baseUrl,
            @Value("${app.external.fifty-plus.api-key}") String apiKey,
            @Value("${app.external.fifty-plus.response-type:json}") String responseType,
            @Value("${app.external.fifty-plus.page-no:1}") int pageNo,
            @Value("${app.external.fifty-plus.num-of-rows:100}") int numOfRows,
            @Value("${app.external.fifty-plus.max-pages:10}") int maxPages,
            @Value("${app.policy.sync.zone-id:Asia/Seoul}") String zoneId
    ) {
        this.api = api;
        this.reader = reader;
        this.dateParser = dateParser;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.responseType = responseType;
        this.pageNo = Math.max(pageNo, 1);
        this.numOfRows = Math.min(Math.max(numOfRows, 1), 100);
        this.maxPages = Math.max(maxPages, 1);
        this.zoneId = zoneId;
    }

    @Override public String sourceName() { return "서울시50플러스-직업역량강화교육"; }
    @Override public String sourceBaseUrl() { return baseUrl; }
    @Override public String sourceCategory() { return "교육"; }

    @Override
    public List<ExternalPolicyItem> fetch() {
        List<ExternalPolicyItem> result = new ArrayList<>();
        int currentYear = Year.now(ZoneId.of(zoneId)).getValue();

        for (int year : List.of(currentYear - 1, currentYear)) {
            for (int page = pageNo; page < pageNo + maxPages; page++) {
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("_type", responseType);
                params.put("accessKey", apiKey);
                params.put("year", year);
                params.put("pageNo", page);
                params.put("numOfRows", numOfRows);

                String raw = api.get(baseUrl, params);
                JsonNode root = api.readJson(raw);
                List<JsonNode> items = reader.findObjectsContainingAny(root, "lctNm");

                if (items.isEmpty()) break;
                items.forEach(item -> result.add(mapItem(item, raw)));
                if (items.size() < numOfRows) break;
            }
        }
        return result;
    }

    private ExternalPolicyItem mapItem(JsonNode item, String rawJson) {
        String title = reader.firstText(item, "lctNm");
        String summary = reader.firstText(item, "lctCtt");
        String agencyName = reader.firstText(item, "orgNm");
        String lecturer = reader.firstText(item, "rpstLctr");
        String courseStart = reader.firstText(item, "crStartDe");
        String courseEnd = reader.firstText(item, "crEndDe");
        String cost = reader.firstText(item, "lctCost");

        String content = reader.joinNonBlank("\n",
                summary,
                courseStart == null ? null : "교육 시작일: " + courseStart,
                courseEnd == null ? null : "교육 종료일: " + courseEnd,
                cost == null ? null : "교육비: " + cost,
                lecturer == null ? null : "강사: " + lecturer
        );

        return ExternalPolicyItem.builder()
                .sourceName(sourceName()).sourceBaseUrl(sourceBaseUrl())
                .externalId(reader.firstText(item, "lctNo", "lctSn", "lctId", "courseId"))
                .title(reader.stripHtml(title))
                .agencyName(reader.stripHtml(agencyName))
                .summary(reader.stripHtml(summary))
                .category(PolicyCategory.EDUCATION)
                .target("서울시 중장년")
                .region("서울특별시")
                .startDate(dateParser.parseSingle(reader.firstText(item, "regStartDe")))
                .endDate(dateParser.parseSingle(reader.firstText(item, "regEndDe")))
                .statusText(reader.firstText(item, "lctStatView"))
                .applyMethod(reader.stripHtml(reader.firstText(item, "lctTypeView")))
                .officialUrl(reader.firstText(item, "lctUrl", "url", "homepageUrl", "link"))
                .contact(reader.stripHtml(reader.joinNonBlank(" / ", agencyName, lecturer)))
                .benefit(reader.stripHtml(summary))
                .contentText(reader.stripHtml(content))
                .rawJson(rawJson).httpStatus(200)
                .build();
    }
}