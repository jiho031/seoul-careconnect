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
import java.util.Locale;

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
        params.put("pageUnit", pageUnit);
        params.put("pageIndex", pageIndex);

        // 선택 파라미터는 값이 존재할 때만 전달
        if (hashtags != null && !hashtags.isBlank()) {
            params.put("hashtags", hashtags.trim());
        }

        if (categoryCode != null && !categoryCode.isBlank()) {
            params.put("searchLclasId", categoryCode.trim());
        }

        String raw = api.get(url, params);
        JsonNode root = api.readJson(raw);

        String requestError = reader.firstText(
                root,
                "reqErr",
                "error",
                "errorMessage",
                "message"
        );

        if (requestError != null && !requestError.isBlank()) {
            throw new IllegalStateException(
                    sourceName() + " API 오류: " + requestError
            );
        }

        JsonNode jsonArray = root.get("jsonArray");

        if (jsonArray == null || !jsonArray.isArray()) {
            String preview = raw.replaceAll("\\s+", " ");

            if (preview.length() > 1000) {
                preview = preview.substring(0, 1000);
            }

            throw new IllegalStateException(
                    sourceName()
                            + " API 응답에 jsonArray 배열이 없습니다. 응답 일부: "
                            + preview
            );
        }

        List<ExternalPolicyItem> result = new ArrayList<>();

        for (JsonNode item : jsonArray) {
            String externalId = reader.firstText(item, "pblancId", "seq");
            String title = reader.firstText(item, "pblancNm", "title");

            if (externalId == null || title == null) {
                continue;
            }

            result.add(mapItem(item, raw));
        }

        return result;
    }

    private ExternalPolicyItem mapItem(JsonNode item, String rawJson) {
        String title = reader.firstText(item, "pblancNm", "title");
        String summary = reader.firstText(item, "bsnsSumryCn", "description");
        String method = reader.firstText(item, "reqstMthPapersCn");
        String target = reader.firstText(item, "trgetNm");
        String category = reader.firstText(item, "pldirSportRealmLclasCodeNm", "lcategory");
        String area = reader.firstText(
                item,
                "areaNm",
                "regionNm"
        );

        String hashTags = reader.firstText(
                item,
                "hashtags",
                "hashTags"
        );

        String jurisdiction = reader.firstText(
                item,
                "jrsdInsttNm",
                "author"
        );

        String executingAgency = reader.firstText(
                item,
                "excInsttNm"
        );

        String region = resolveBizInfoRegion(
                area,
                title,
                jurisdiction,
                executingAgency
        );

        String agencyName = reader.joinNonBlank(
                " / ",
                jurisdiction,
                executingAgency
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
                .region(region)
                .district(
                        "서울특별시".equals(region)
                                ? classifier.district(
                                area,
                                title,
                                summary,
                                jurisdiction,
                                executingAgency
                        )
                                : null
                )
                .startDate(range.startDate())
                .endDate(range.endDate())
                .statusText(reader.firstText(item, "status", "rceptSttus"))
                .applyMethod(reader.stripHtml(method))
                .officialUrl(
                        reader.firstText(
                                item,
                                "pblancUrl",
                                "link",
                                "rceptEngnHmpgUrl"
                        )
                )
                .contact(reader.stripHtml(reader.joinNonBlank(" / ",
                        agencyName, reader.firstText(item, "refrncNm")
                )))
                .benefit(reader.stripHtml(summary))
                .requiredDocumentsText(
                        extractApplicationDocuments(
                                reader.firstText(item, "fileNm"),
                                reader.firstText(item, "printFileNm")
                        )
                )
                .contentText(reader.stripHtml(reader.joinNonBlank(
                        "\n",
                        summary,
                        target,
                        method
                )))
                .rawJson(rawJson)
                .httpStatus(200)
                .build();
    }

    private String extractApplicationDocuments(
            String... fileNameGroups
    ) {
        List<String> documents =
                new ArrayList<>();

        for (String group : fileNameGroups) {

            if (group == null
                    || group.isBlank()) {
                continue;
            }

            for (String fileName :
                    group.split("@|\\r?\\n")) {

                String cleaned =
                        reader.stripHtml(fileName);

                if (cleaned != null
                        && isApplicationDocumentName(cleaned)
                        && !documents.contains(cleaned)) {

                    documents.add(cleaned);
                }
            }
        }

        return documents.isEmpty()
                ? null
                : String.join("\n", documents);
    }

    private boolean isApplicationDocumentName(
            String fileName
    ) {
        String normalized =
                fileName
                        .replaceAll("\\s+", "")
                        .toLowerCase(Locale.ROOT);

        if (containsAny(
                normalized,
                "공고문",
                "모집안내",
                "사업안내",
                "추진계획",
                "포스터",
                "홍보"
        )) {
            return false;
        }

        return containsAny(
                normalized,
                "신청서",
                "신청양식",
                "제출서류",
                "제출서식",
                "서식",
                "양식",
                "동의서",
                "서약서",
                "신고서",
                "확인서",
                "증빙"
        );
    }

    private String resolveBizInfoRegion(String... values) {
        String text = reader.joinNonBlank(" ", values);

        if (text == null || text.isBlank()) {
            return "전국";
        }

        String normalized = text.replaceAll("\\s+", "");

        if (containsAny(normalized, "서울특별시", "서울시", "[서울]")) {
            return "서울특별시";
        }

        if (containsAny(normalized, "경기도", "[경기]")) {
            return "경기도";
        }

        if (containsAny(normalized, "인천광역시", "[인천]")) {
            return "인천광역시";
        }

        if (containsAny(normalized, "부산광역시", "[부산]")) {
            return "부산광역시";
        }

        if (containsAny(normalized, "대구광역시", "[대구]")) {
            return "대구광역시";
        }

        if (containsAny(normalized, "광주광역시", "[광주]", "[전남광주]")) {
            return "광주광역시";
        }

        if (containsAny(normalized, "대전광역시", "[대전]")) {
            return "대전광역시";
        }

        if (containsAny(normalized, "울산광역시", "[울산]")) {
            return "울산광역시";
        }

        if (containsAny(normalized, "세종특별자치시", "[세종]")) {
            return "세종특별자치시";
        }

        if (containsAny(
                normalized,
                "강원특별자치도",
                "강원도",
                "강원영동",
                "영동권",
                "[강원]"
        )) {
            return "강원특별자치도";
        }

        if (containsAny(normalized, "충청북도", "[충북]")) {
            return "충청북도";
        }

        if (containsAny(normalized, "충청남도", "[충남]")) {
            return "충청남도";
        }

        if (containsAny(normalized, "전북특별자치도", "전라북도", "[전북]")) {
            return "전북특별자치도";
        }

        if (containsAny(normalized, "전라남도", "[전남]")) {
            return "전라남도";
        }

        if (containsAny(normalized, "경상북도", "[경북]")) {
            return "경상북도";
        }

        if (containsAny(normalized, "경상남도", "[경남]")) {
            return "경상남도";
        }

        if (containsAny(normalized, "제주특별자치도", "제주도", "[제주]")) {
            return "제주특별자치도";
        }

        /*
         * 지역 제한이 확인되지 않는 중앙부처 사업은
         * 서울 사용자도 신청 가능한 전국 사업으로 처리합니다.
         */
        return "전국";
    }

    private boolean containsAny(
            String text,
            String... keywords
    ) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}