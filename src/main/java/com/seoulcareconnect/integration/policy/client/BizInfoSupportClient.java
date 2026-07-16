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
@ConditionalOnProperty(name = "app.external.bizinfo.support.enabled", havingValue = "true")
public class BizInfoSupportClient implements ExternalPolicyClient {

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

    public BizInfoSupportClient(
            ExternalApiSupport api,
            ExternalNodeReader reader,
            ExternalDateParser dateParser,
            ExternalPolicyClassifier classifier,
            @Value("${app.external.bizinfo.support.api-key}") String apiKey,
            @Value("${app.external.bizinfo.data-type:json}") String dataType,
            @Value("${app.external.bizinfo.search-count:100}") int searchCount,
            @Value("${app.external.bizinfo.page-unit:100}") int pageUnit,
            @Value("${app.external.bizinfo.page-index:1}") int pageIndex,
            @Value("${app.external.bizinfo.hashtags:서울}") String hashtags,
            @Value("${app.external.bizinfo.support.category-code:}") String categoryCode,
            @Value("${app.external.bizinfo.support.url}") String url
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

    @Override public String sourceName() { return "기업마당-지원사업정보"; }
    @Override public String sourceBaseUrl() { return url; }
    @Override public String sourceCategory() { return "기업지원"; }

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
        List<JsonNode> items = reader.findObjectsContainingAny(root, "pblancId", "seq").stream()
                .filter(item -> reader.firstText(item, "pblancNm", "title") != null)
                .toList();

        List<ExternalPolicyItem> result = new ArrayList<>();
        items.forEach(item -> result.add(mapItem(item, raw)));
        return result;
    }

    private ExternalPolicyItem mapItem(JsonNode item, String rawJson) {
        String title = reader.firstText(item, "pblancNm", "title");
        String summary = reader.firstText(item, "bsnsSumryCn", "description");
        String method = reader.firstText(item, "reqstMthPapersCn");
        String target = reader.firstText(item, "trgetNm");
        String category = reader.firstText(item, "pldirSportRealmLclasCodeNm", "lcategory");
        String area = reader.firstText(item, "areaNm", "regionNm");
        String hashTags = reader.firstText(item, "hashTags");
        String agencyName = reader.joinNonBlank(" / ",
                reader.firstText(item, "jrsdInsttNm", "author"),
                reader.firstText(item, "excInsttNm")
        );

        ExternalDateParser.DateRange range = dateParser.parseRange(
                reader.firstText(item, "reqstBeginEndDe", "reqstDt")
        );

        return ExternalPolicyItem.builder()
                .sourceName(sourceName()).sourceBaseUrl(sourceBaseUrl())
                .externalId(reader.firstText(item, "pblancId", "seq"))
                .title(reader.stripHtml(title))
                .agencyName(reader.stripHtml(agencyName))
                .summary(reader.stripHtml(summary))
                .category(classifier.category(PolicyCategory.JOB, category, title, summary))
                .target(reader.stripHtml(target))
                .region(area == null ? "서울특별시" : area)
                .district(classifier.district(area, hashTags, title, summary))
                .startDate(range.startDate())
                .endDate(range.endDate())
                .statusText(reader.firstText(item, "status", "rceptSttus"))
                .applyMethod(reader.stripHtml(method))
                .officialUrl(reader.firstText(item, "rceptEngnHmpgUrl", "pblancUrl", "link"))
                .contact(reader.stripHtml(reader.joinNonBlank(" / ",
                        agencyName, reader.firstText(item, "refrncNm")
                )))
                .benefit(reader.stripHtml(summary))
                .requiredDocumentsText(reader.stripHtml(method))
                .contentText(reader.stripHtml(reader.joinNonBlank("\n",
                        summary, target, method, agencyName, hashTags
                )))
                .rawJson(rawJson).httpStatus(200)
                .build();
    }
}