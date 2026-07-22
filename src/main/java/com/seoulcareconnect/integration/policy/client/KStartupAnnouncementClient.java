package com.seoulcareconnect.integration.policy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.integration.policy.ExternalApiSupport;
import com.seoulcareconnect.integration.policy.ExternalDateParser;
import com.seoulcareconnect.integration.policy.ExternalNodeReader;
import com.seoulcareconnect.integration.policy.ExternalPolicyClassifier;
import com.seoulcareconnect.integration.policy.ExternalPolicyClient;
import com.seoulcareconnect.integration.policy.ExternalPolicyItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "app.external.data-go.kstartup.enabled",
        havingValue = "true"
)
public class KStartupAnnouncementClient implements ExternalPolicyClient {

    private final ExternalApiSupport api;
    private final ExternalNodeReader reader;
    private final ExternalDateParser dateParser;
    private final ExternalPolicyClassifier classifier;
    private final String serviceKey;
    private final String url;
    private final String returnType;
    private final String targetAge;
    private final int pageNo;
    private final int perPage;
    private final int maxPages;
    private final int maxResults;

    public KStartupAnnouncementClient(
            ExternalApiSupport api,
            ExternalNodeReader reader,
            ExternalDateParser dateParser,
            ExternalPolicyClassifier classifier,
            @Value("${app.external.data-go.service-key}") String serviceKey,
            @Value("${app.external.data-go.kstartup.url}") String url,
            @Value("${app.external.data-go.kstartup.return-type:json}") String returnType,
            @Value("${app.external.data-go.kstartup.target-age:만 40세 이상}") String targetAge,
            @Value("${app.external.data-go.page-no:1}") int pageNo,
            @Value("${app.external.data-go.num-of-rows:100}") int perPage,
            @Value("${app.external.data-go.kstartup.max-pages:1}") int maxPages,
            @Value("${app.external.data-go.kstartup.max-results:20}") int maxResults
    ) {
        this.api = api;
        this.reader = reader;
        this.dateParser = dateParser;
        this.classifier = classifier;
        this.serviceKey = serviceKey;
        this.url = url;
        this.returnType = returnType;
        this.targetAge = targetAge == null ? "" : targetAge.trim();
        this.pageNo = Math.max(pageNo, 1);
        this.perPage = Math.min(Math.max(perPage, 1), 100);
        this.maxPages = Math.max(maxPages, 1);
        this.maxResults = Math.min(Math.max(maxResults, 1), 100);
    }

    @Override
    public String sourceName() {
        return "K-Startup-지원사업공고";
    }

    @Override
    public String sourceBaseUrl() {
        return url;
    }

    @Override
    public String sourceCategory() {
        return "창업·일자리";
    }

    @Override
    public List<ExternalPolicyItem> fetch() {
        List<ExternalPolicyItem> result = new ArrayList<>();

        for (int page = pageNo;
             page < pageNo + maxPages && result.size() < maxResults;
             page++) {

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("serviceKey", serviceKey);
            params.put("page", page);
            params.put("perPage", perPage);
            params.put("cond[rcrt_prgs_yn::EQ]", "Y");
            params.put("returnType", returnType);

            String raw = api.get(url, params);
            validateJsonResponse(raw);

            JsonNode root = api.readJson(raw);
            List<JsonNode> items = reader.findObjectsContainingAny(
                    root,
                    "biz_pbanc_nm",
                    "intg_pbanc_biz_nm",
                    "pbanc_nm"
            );

            log.info(
                    "K-Startup API 페이지 응답 확인: page={}, 원본 공고수={}",
                    page,
                    items.size()
            );

            if (items.isEmpty()) {
                if (page == pageNo) {
                    throw new IllegalStateException(
                            "K-Startup 응답에서 공고 항목을 찾지 못했습니다. 응답 앞부분="
                                    + preview(raw)
                    );
                }
                break;
            }

            int matchedCount = 0;

            for (JsonNode item : items) {
                if (!isTargetAgeMatched(item)) {
                    continue;
                }

                result.add(mapItem(item, raw));
                matchedCount++;

                if (result.size() >= maxResults) {
                    break;
                }
            }

            log.info(
                    "K-Startup 중장년 조건 처리: page={}, 조건통과={}, 누적후보={}",
                    page,
                    matchedCount,
                    result.size()
            );

            if (items.size() < perPage) {
                break;
            }
        }

        if (result.isEmpty()) {
            throw new IllegalStateException(
                    "K-Startup 공고는 조회됐지만 중장년 대상 조건에 맞는 공고가 0건입니다. "
                            + "targetAge=" + targetAge
            );
        }

        return result;
    }

    private ExternalPolicyItem mapItem(JsonNode item, String rawJson) {
        String title = reader.firstText(
                item,
                "biz_pbanc_nm",
                "intg_pbanc_biz_nm",
                "pbanc_nm",
                "title"
        );

        String applicationTarget = reader.firstText(
                item,
                "aply_trgt",
                "aply_trgt_ctnt",
                "biz_supt_trgt_info"
        );

        String businessAge = reader.firstText(
                item,
                "biz_enyy",
                "business_age"
        );

        String targetAgeText = reader.firstText(
                item,
                "biz_trgt_age",
                "target_age"
        );

        String target = reader.joinNonBlank(
                " | ",
                applicationTarget,
                businessAge,
                targetAgeText
        );

        String field = reader.firstText(
                item,
                "supt_biz_clsfc",
                "biz_category_nm",
                "supt_biz_titl_nm"
        );

        String summary = reader.firstText(
                item,
                "pbanc_ctnt",
                "supt_ctnt",
                "biz_supt_ctnt",
                "description"
        );

        String method = reader.firstText(
                item,
                "aply_mthd",
                "aply_mthd_ctnt",
                "reqstMthPapersCn"
        );

        String region = reader.firstText(
                item,
                "supt_regin",
                "region_nm",
                "areaNm"
        );

        String agencyName = reader.firstText(
                item,
                "pbanc_ntrp_nm",
                "biz_supt_org_nm",
                "jrsd_instt_nm"
        );

        String contactInfo = reader.firstText(
                item,
                "detl_cnsl_guide",
                "contact",
                "tel_no"
        );

        String content = reader.joinNonBlank(
                "\n",
                field,
                target,
                summary,
                method
        );

        return ExternalPolicyItem.builder()
                .sourceName(sourceName())
                .sourceBaseUrl(sourceBaseUrl())
                .externalId(reader.firstText(
                        item,
                        "pbanc_sn",
                        "biz_pbanc_sn",
                        "intg_pbanc_sn",
                        "id"
                ))
                .title(reader.stripHtml(title))
                .agencyName(reader.stripHtml(agencyName))
                .summary(reader.stripHtml(summary))
                .category(classifier.category(
                        PolicyCategory.JOB,
                        field,
                        title,
                        summary,
                        target
                ))
                .target(reader.stripHtml(target))
                .region(reader.stripHtml(region))
                .district(classifier.district(region, title, summary))
                .startDate(dateParser.parseSingle(reader.firstText(
                        item,
                        "pbanc_rcpt_bgng_dt",
                        "rcpt_bgng_dt"
                )))
                .endDate(dateParser.parseSingle(reader.firstText(
                        item,
                        "pbanc_rcpt_end_dt",
                        "rcpt_end_dt"
                )))
                .statusText(reader.firstText(item, "rcrt_prgs_yn", "status"))
                .applyMethod(reader.stripHtml(method))
                .officialUrl(reader.firstText(
                        item,
                        "detl_pg_url",
                        "pbanc_url",
                        "aply_url",
                        "url"
                ))
                .contact(reader.stripHtml(reader.joinNonBlank(
                        " / ",
                        agencyName,
                        contactInfo
                )))
                .benefit(reader.stripHtml(summary))
                .selectionCriteria(reader.stripHtml(target))
                .requiredDocumentsText(null)
                .contentText(reader.stripHtml(content))
                .rawJson(rawJson)
                .httpStatus(200)
                .build();
    }

    private boolean isTargetAgeMatched(JsonNode item) {
        if (targetAge.isBlank()) {
            return true;
        }

        String age = reader.firstText(
                item,
                "biz_trgt_age",
                "target_age"
        );

        String normalizedAge = normalize(age);

        if (normalizedAge.isBlank()) {
            return true;
        }

        if (normalize(targetAge).contains("40세이상")) {

            if (containsAny(
                    normalizedAge,
                    "만40세이상",
                    "40세이상",
                    "40대",
                    "50대",
                    "60대",
                    "중장년",
                    "중년",
                    "신중년",
                    "전연령",
                    "연령제한없음",
                    "제한없음",
                    "누구나"
            )) {
                return true;
            }

            return !containsAny(
                    normalizedAge,
                    "만39세이하",
                    "39세이하",
                    "만20세이상~만39세이하",
                    "청년전용"
            );
        }

        return normalizedAge.contains(
                normalize(targetAge)
        );
    }

    private void validateJsonResponse(String raw) {
        String normalized = raw == null ? "" : raw.stripLeading();

        if (normalized.startsWith("{") || normalized.startsWith("[")) {
            return;
        }

        throw new IllegalStateException(
                "K-Startup이 JSON이 아닌 응답을 반환했습니다. 응답 앞부분="
                        + preview(raw)
        );
    }

    private String preview(String raw) {
        if (raw == null || raw.isBlank()) {
            return "(응답 없음)";
        }

        String normalized = raw
                .replaceAll("\\s+", " ")
                .trim();

        return normalized.substring(
                0,
                Math.min(normalized.length(), 1000)
        );
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .trim();
    }
}