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
@ConditionalOnProperty(name = "app.external.data-go.kstartup.enabled", havingValue = "true")
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
            @Value("${app.external.data-go.kstartup.max-pages:5}") int maxPages
    ) {
        this.api = api;
        this.reader = reader;
        this.dateParser = dateParser;
        this.classifier = classifier;
        this.serviceKey = serviceKey;
        this.url = url;
        this.returnType = returnType;
        this.targetAge = targetAge;
        this.pageNo = Math.max(pageNo, 1);
        this.perPage = Math.min(Math.max(perPage, 1), 100);
        this.maxPages = Math.max(maxPages, 1);
    }

    @Override public String sourceName() { return "K-Startup-지원사업공고"; }
    @Override public String sourceBaseUrl() { return url; }
    @Override public String sourceCategory() { return "창업·일자리"; }

    @Override
    public List<ExternalPolicyItem> fetch() {
        List<ExternalPolicyItem> result = new ArrayList<>();

        for (int page = pageNo; page < pageNo + maxPages; page++) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("serviceKey", serviceKey);
            params.put("page", page);
            params.put("perPage", perPage);
            params.put("cond[biz_trgt_age::LIKE]", targetAge);
            params.put("cond[rcrt_prgs_yn::EQ]", "Y");
            params.put("returnType", returnType);

            String raw = api.get(url, params);
            JsonNode root = api.readJson(raw);
            List<JsonNode> items = reader.findObjectsContainingAny(
                    root, "biz_pbanc_nm", "intg_pbanc_biz_nm", "pbanc_nm"
            );

            if (items.isEmpty()) break;
            items.forEach(item -> result.add(mapItem(item, raw)));
            if (items.size() < perPage) break;
        }
        return result;
    }

    private ExternalPolicyItem mapItem(JsonNode item, String rawJson) {
        String title = reader.firstText(item, "biz_pbanc_nm", "intg_pbanc_biz_nm", "pbanc_nm", "title");
        String target = reader.firstText(item, "aply_trgt_ctnt", "aply_trgt", "biz_supt_trgt_info");
        String field = reader.firstText(item, "supt_biz_clsfc", "biz_category_nm", "supt_biz_titl_nm");
        String summary = reader.firstText(item, "supt_ctnt", "biz_supt_ctnt", "pbanc_ctnt", "description");
        String method = reader.firstText(item, "aply_mthd", "aply_mthd_ctnt", "reqstMthPapersCn");
        String region = reader.firstText(item, "supt_regin", "region_nm", "areaNm");
        String agencyName = reader.firstText(item, "pbanc_ntrp_nm", "biz_supt_org_nm", "jrsd_instt_nm");
        String contactInfo = reader.firstText(item, "detl_cnsl_guide", "contact", "tel_no");

        return ExternalPolicyItem.builder()
                .sourceName(sourceName()).sourceBaseUrl(sourceBaseUrl())
                .externalId(reader.firstText(item, "biz_pbanc_sn", "pbanc_sn", "intg_pbanc_sn", "id"))
                .title(reader.stripHtml(title))
                .agencyName(reader.stripHtml(agencyName))
                .summary(reader.stripHtml(summary))
                .category(classifier.category(PolicyCategory.JOB, field, title, summary))
                .target(reader.stripHtml(target))
                .region(reader.stripHtml(region))
                .district(classifier.district(region, title, summary))
                .startDate(dateParser.parseSingle(reader.firstText(item, "pbanc_rcpt_bgng_dt", "rcpt_bgng_dt")))
                .endDate(dateParser.parseSingle(reader.firstText(item, "pbanc_rcpt_end_dt", "rcpt_end_dt")))
                .statusText(reader.firstText(item, "rcrt_prgs_yn", "status"))
                .applyMethod(reader.stripHtml(method))
                .officialUrl(reader.firstText(item, "detl_pg_url", "pbanc_url", "aply_url", "url"))
                .contact(reader.stripHtml(reader.joinNonBlank(" / ", agencyName, contactInfo)))
                .benefit(reader.stripHtml(summary))
                .requiredDocumentsText(reader.stripHtml(method))
                .contentText(reader.stripHtml(reader.joinNonBlank("\n", field, target, summary, method)))
                .rawJson(rawJson).httpStatus(200)
                .build();
    }
}