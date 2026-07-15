package com.seoulcareconnect.integration.policy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.integration.policy.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

abstract class AbstractMyHomeClient implements ExternalPolicyClient {

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
            String serviceKey, String url, String regionCode,
            int pageNo, int numOfRows, int maxPages
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

    @Override public String sourceBaseUrl() { return url; }
    @Override public String sourceCategory() { return "주거"; }

    @Override
    public List<ExternalPolicyItem> fetch() {
        List<ExternalPolicyItem> result = new ArrayList<>();

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
                    root, "rcritPblancNm", "pblancNm", "panNm", "hsmpNm"
            );

            if (items.isEmpty()) break;
            items.forEach(item -> result.add(mapItem(item, raw)));
            if (items.size() < numOfRows) break;
        }
        return result;
    }

    private ExternalPolicyItem mapItem(JsonNode item, String rawJson) {
        String title = reader.firstText(item, "rcritPblancNm", "pblancNm", "panNm", "hsmpNm", "title");
        String target = reader.firstText(item, "suplyTrget", "trgetNm", "houseTyNm", "suplyTyNm");
        String summary = reader.firstText(item, "dtlCn", "pblancCn", "rm", "description");
        String agencyName = reader.firstText(item, "insttNm", "suplyInsttNm", "agency");
        String phone = reader.firstText(item, "inqireTel", "telNo", "contact");
        String address = reader.firstText(item, "adres", "address");

        String region = reader.joinNonBlank(" ",
                reader.firstText(item, "brtcNm", "sidoNm"),
                reader.firstText(item, "signguNm", "sggNm")
        );

        String content = reader.joinNonBlank("\n", target, summary, address);
        ExternalDateParser.DateRange range = dateParser.parseRange(
                reader.firstText(item, "rcritBeginEndDe", "reqstBeginEndDe", "rceptPd")
        );

        return ExternalPolicyItem.builder()
                .sourceName(sourceName()).sourceBaseUrl(sourceBaseUrl())
                .externalId(reader.firstText(item, "rcritNtcId", "pblancId", "panId", "hsmpSn", "seq"))
                .title(reader.stripHtml(title))
                .agencyName(reader.stripHtml(agencyName))
                .summary(reader.stripHtml(summary))
                .category(PolicyCategory.HOUSING)
                .target(reader.stripHtml(target))
                .region(region == null ? "서울특별시" : region)
                .district(classifier.district(region, address, summary))
                .startDate(range.startDate() != null ? range.startDate()
                        : dateParser.parseSingle(reader.firstText(item, "rcritBgngDe", "beginDe", "startDate")))
                .endDate(range.endDate() != null ? range.endDate()
                        : dateParser.parseSingle(reader.firstText(item, "rcritEndDe", "endDe", "endDate")))
                .statusText(reader.firstText(item, "rcritSttusNm", "status", "rcrt_prgs_yn"))
                .applyMethod(reader.stripHtml(reader.firstText(item, "reqstMth", "aplyMthd", "applyMethod")))
                .officialUrl(reader.firstText(item, "pblancUrl", "dtlUrl", "url", "link"))
                .contact(reader.stripHtml(reader.joinNonBlank(" / ", agencyName, phone)))
                .benefit(reader.stripHtml(summary))
                .contentText(reader.stripHtml(content))
                .rawJson(rawJson).httpStatus(200)
                .build();
    }
}