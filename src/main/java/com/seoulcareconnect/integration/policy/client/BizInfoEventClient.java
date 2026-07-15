package com.seoulcareconnect.integration.policy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.integration.policy.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.external.bizinfo.event.enabled", havingValue = "true")
public class BizInfoEventClient implements ExternalPolicyClient {

    private final ExternalApiSupport api;
    private final ExternalNodeReader reader;
    private final ExternalDateParser dateParser;
    private final ExternalPolicyClassifier classifier;
    private final String apiKey;
    private final String dataType;
    private final int searchCount;
    private final int pageUnit;
    private final int pageIndex;
    private final String hashtags;
    private final String categoryCode;
    private final String url;

    public BizInfoEventClient(
            ExternalApiSupport api,
            ExternalNodeReader reader,
            ExternalDateParser dateParser,
            ExternalPolicyClassifier classifier,
            @Value("${app.external.bizinfo.event.api-key}") String apiKey,
            @Value("${app.external.bizinfo.data-type:json}") String dataType,
            @Value("${app.external.bizinfo.search-count:100}") int searchCount,
            @Value("${app.external.bizinfo.page-unit:100}") int pageUnit,
            @Value("${app.external.bizinfo.page-index:1}") int pageIndex,
            @Value("${app.external.bizinfo.hashtags:서울}") String hashtags,
            @Value("${app.external.bizinfo.event.category-code:}") String categoryCode,
            @Value("${app.external.bizinfo.event.url}") String url
    ) {
        this.api = api;
        this.reader = reader;
        this.dateParser = dateParser;
        this.classifier = classifier;
        this.apiKey = apiKey;
        this.dataType = dataType;
        this.searchCount = Math.max(searchCount, 1);
        this.pageUnit = Math.max(pageUnit, 1);
        this.pageIndex = Math.max(pageIndex, 1);
        this.hashtags = hashtags;
        this.categoryCode = categoryCode;
        this.url = url;
    }

    @Override public String sourceName() { return "기업마당-행사정보"; }
    @Override public String sourceBaseUrl() { return url; }
    @Override public String sourceCategory() { return "행사·교육"; }

    @Override
    public List<ExternalPolicyItem> fetch() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("crtfcKey", apiKey);
        params.put("dataType", dataType);
        params.put("searchCnt", searchCount);
        params.put("hashtags", hashtags);
        params.put("pageUnit", pageUnit);
        params.put("pageIndex", pageIndex);
        params.put("searchLclasId", categoryCode);

        String raw = api.get(url, params);
        JsonNode root = api.readJson(raw);
        List<JsonNode> items = reader.findObjectsContainingAny(root, "eventInfoId", "seq").stream()
                .filter(item -> reader.firstText(item, "nttNm", "title") != null)
                .toList();

        List<ExternalPolicyItem> result = new ArrayList<>();
        items.forEach(item -> result.add(mapItem(item, raw)));
        return result;
    }

    private ExternalPolicyItem mapItem(JsonNode item, String rawJson) {
        String title = reader.firstText(item, "nttNm", "title");
        String summary = reader.firstText(item, "nttCn", "description");
        String eventType = reader.firstText(item, "eventInfoTyNm", "eventType");
        String category = reader.firstText(item, "pldirSportRealmLclasCodeNm", "lcategory");
        String area = reader.firstText(item, "areaNm", "regionNm");
        String agencyName = reader.firstText(item, "originEngnNm", "originOrg");
        String reference = reader.firstText(item, "refrncNm");

        ExternalDateParser.DateRange receiptRange = dateParser.parseRange(
                reader.firstText(item, "rceptPd")
        );

        ExternalDateParser.DateRange eventRange = dateParser.parseRange(
                reader.firstText(item, "BeginEndDe", "eventPeriod")
        );

        return ExternalPolicyItem.builder()
                .sourceName(sourceName()).sourceBaseUrl(sourceBaseUrl())
                .externalId(reader.firstText(item, "eventInfoId", "seq"))
                .title(reader.stripHtml(title))
                .agencyName(reader.stripHtml(agencyName))
                .summary(reader.stripHtml(summary))
                .category(classifier.category(PolicyCategory.EDUCATION, eventType, category, title, summary))
                .target("중소기업 및 예비 창업자")
                .region(area == null ? "서울특별시" : area)
                .district(classifier.district(area, title, summary))
                .startDate(receiptRange.startDate() != null
                        ? receiptRange.startDate() : eventRange.startDate())
                .endDate(receiptRange.endDate() != null
                        ? receiptRange.endDate() : eventRange.endDate())
                .applyMethod("행사 안내 페이지에서 신청 방법 확인")
                .officialUrl(reader.firstText(item, "originUrlAdres", "originUrl", "bizinfoUrl"))
                .contact(reader.stripHtml(reader.joinNonBlank(" / ", agencyName, reference)))
                .benefit(reader.stripHtml(summary))
                .contentText(reader.stripHtml(reader.joinNonBlank("\n",
                        summary, eventType,
                        reader.firstText(item, "BeginEndDe", "eventPeriod"),
                        agencyName
                )))
                .rawJson(rawJson).httpStatus(200)
                .build();
    }
}