package com.seoulcareconnect.integration.policy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.integration.policy.ExternalApiSupport;
import com.seoulcareconnect.integration.policy.ExternalNodeReader;
import com.seoulcareconnect.integration.policy.ExternalPolicyClassifier;
import com.seoulcareconnect.integration.policy.ExternalPolicyClient;
import com.seoulcareconnect.integration.policy.ExternalPolicyItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(
        name = "app.external.seoul-open-data.welfare.enabled",
        havingValue = "true"
)
public class SeoulOpenDataWelfareClient implements ExternalPolicyClient {

    private final ExternalApiSupport api;
    private final ExternalNodeReader reader;
    private final ExternalPolicyClassifier classifier;
    private final String apiKey;
    private final String baseUrl;
    private final String responseType;
    private final String serviceName;
    private final int startIndex;
    private final int pageSize;
    private final int maxPages;

    public SeoulOpenDataWelfareClient(
            ExternalApiSupport api,
            ExternalNodeReader reader,
            ExternalPolicyClassifier classifier,
            @Value("${app.external.seoul-open-data.api-key}") String apiKey,
            @Value("${app.external.seoul-open-data.base-url:http://openapi.seoul.go.kr:8088}") String baseUrl,
            @Value("${app.external.seoul-open-data.response-type:json}") String responseType,
            @Value("${app.external.seoul-open-data.welfare.service-name:welfareServiceDe}") String serviceName,
            @Value("${app.external.seoul-open-data.welfare.start-index:1}") int startIndex,
            @Value("${app.external.seoul-open-data.welfare.page-size:1000}") int pageSize,
            @Value("${app.external.seoul-open-data.welfare.max-pages:1}") int maxPages
    ) {
        this.api = api;
        this.reader = reader;
        this.classifier = classifier;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.responseType = responseType;
        this.serviceName = serviceName;
        this.startIndex = Math.max(startIndex, 1);
        this.pageSize = Math.min(Math.max(pageSize, 1), 1000);
        this.maxPages = Math.max(maxPages, 1);
    }

    @Override
    public String sourceName() {
        return "서울열린데이터-복지서비스상세";
    }

    @Override
    public String sourceBaseUrl() {
        return baseUrl;
    }

    @Override
    public String sourceCategory() {
        return "복지";
    }

    @Override
    public List<ExternalPolicyItem> fetch() {
        validateConfiguration();

        Map<String, ExternalPolicyItem> result = new LinkedHashMap<>();

        for (int page = 0; page < maxPages; page++) {
            int start = startIndex + page * pageSize;
            int end = start + pageSize - 1;
            String raw = api.get(buildRequestUrl(start, end), Map.of());
            JsonNode root = parse(raw);
            validateResult(root);

            List<JsonNode> rows = reader.findObjectsContainingAny(root, "SERV_ID");
            for (JsonNode row : rows) {
                ExternalPolicyItem item = mapItem(row);
                if (item.getExternalId() != null) {
                    result.put(item.getExternalId(), item);
                }
            }

            Integer totalCount = totalCount(root);
            if (rows.size() < pageSize || totalCount != null && end >= totalCount) {
                break;
            }
        }

        if (result.isEmpty()) {
            throw new IllegalStateException("서울 열린데이터 복지서비스 응답에 정책 행이 없습니다.");
        }

        return new ArrayList<>(result.values());
    }

    private ExternalPolicyItem mapItem(JsonNode row) {
        String servId = reader.firstText(row, "SERV_ID");
        String title = reader.firstText(row, "SERV_NM");
        String summary = reader.firstText(row, "SERV_DGST");
        String lifeCycle = reader.firstText(row, "LIFE_NM_ARRAY");
        String targetGroup = reader.firstText(row, "TRGTER_INDVDL_NM_ARRAY");
        String supportTarget = reader.firstText(row, "SPRT_TRGT_CN");
        String selectionCriteria = reader.firstText(row, "SLCT_CRIT_CN");
        String benefit = reader.firstText(row, "ALW_SERV_CN");
        String applyMethod = reader.joinNonBlank("\n",
                reader.firstText(row, "APLY_MTD_NM"),
                reader.firstText(row, "APLY_MTD_CN")
        );
        String target = reader.joinNonBlank("\n", lifeCycle, targetGroup, supportTarget);
        String theme = reader.firstText(row, "INTRS_THEMA_NM_ARRAY");
        String lastModified = reader.firstText(row, "LAST_MOD_YMD");
        String content = reader.joinNonBlank("\n",
                summary,
                supportTarget,
                selectionCriteria,
                benefit,
                applyMethod,
                lastModified == null ? null : "최종 수정일: " + lastModified
        );

        return ExternalPolicyItem.builder()
                .sourceName(sourceName())
                .sourceBaseUrl(sourceBaseUrl())
                .externalId(servId)
                .title(reader.stripHtml(title))
                .agencyName(reader.stripHtml(reader.firstText(row, "BIZ_CHR_DEPT_NM")))
                .summary(reader.stripHtml(summary))
                .category(classifier.category(
                        PolicyCategory.LIVING_SUPPORT,
                        title,
                        theme,
                        target,
                        benefit
                ))
                .target(reader.stripHtml(target))
                .region(reader.firstText(row, "CTPV_NM"))
                .district(reader.firstText(row, "SGG_NM"))
                .statusText(resolveStatus(row))
                .applyMethod(reader.stripHtml(applyMethod))
                .officialUrl(resolveOfficialUrl(row, servId))
                .contact(reader.stripHtml(reader.firstText(row, "INQ_NUM")))
                .benefit(reader.stripHtml(benefit))
                .selectionCriteria(reader.stripHtml(selectionCriteria))
                .contentText(reader.stripHtml(content))
                .applicationInfoAvailable(false)
                .rawJson(row.toString())
                .httpStatus(200)
                .build();
    }

    private String resolveOfficialUrl(
            JsonNode row,
            String servId
    ) {
        String provided =
                reader.firstText(
                        row,
                        "SERV_DTL_LINK",
                        "SERV_URL",
                        "LINK"
                );

        if (provided != null
                && !provided.isBlank()) {

            return provided;
        }

        if (servId == null
                || servId.isBlank()) {

            return null;
        }

        return "https://www.bokjiro.go.kr/"
                + "ssis-tbu/twataa/wlfareInfo/"
                + "moveTWAT52011M.do?"
                + "wlfareInfoId="
                + servId
                + "&wlfareInfoReldBztpCd=02";
    }

    private String resolveStatus(JsonNode row) {
        String endDate = reader.firstText(row, "ENFC_END_YMD");
        if (endDate == null) {
            return "신청 정보 확인 필요";
        }

        String digits = endDate.replaceAll("[^0-9]", "");
        if ("99991231".equals(digits) || "29991231".equals(digits)) {
            return "시행 중";
        }

        return "시행 종료일 " + endDate;
    }

    private JsonNode parse(String raw) {
        return "xml".equalsIgnoreCase(responseType)
                ? api.readXml(raw)
                : api.readJson(raw);
    }

    private void validateResult(JsonNode root) {
        String code = reader.findObjectsContainingAny(root, "CODE").stream()
                .map(node -> reader.firstText(node, "CODE"))
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);

        if (code != null && !"INFO-000".equalsIgnoreCase(code)) {
            String message = reader.findObjectsContainingAny(root, "MESSAGE").stream()
                    .map(node -> reader.firstText(node, "MESSAGE"))
                    .filter(value -> value != null)
                    .findFirst()
                    .orElse("오류 메시지 없음");
            throw new IllegalStateException("서울 열린데이터 API 오류: " + code + " - " + message);
        }
    }

    private Integer totalCount(JsonNode root) {
        String value = reader.findObjectsContainingAny(root, "list_total_count").stream()
                .map(node -> reader.firstText(node, "list_total_count"))
                .filter(text -> text != null)
                .findFirst()
                .orElse(null);

        if (value == null) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String buildRequestUrl(int start, int end) {
        String normalizedBase = baseUrl.replaceAll("/+$", "");
        return String.format(
                Locale.ROOT,
                "%s/%s/%s/%s/%d/%d/",
                normalizedBase,
                apiKey,
                responseType,
                serviceName,
                start,
                end
        );
    }

    private void validateConfiguration() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("서울 열린데이터 API 키가 비어 있습니다.");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("서울 열린데이터 API 기본 URL이 비어 있습니다.");
        }
        if (!"json".equalsIgnoreCase(responseType) && !"xml".equalsIgnoreCase(responseType)) {
            throw new IllegalStateException("서울 열린데이터 응답 형식은 json 또는 xml이어야 합니다.");
        }
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalStateException("서울 열린데이터 서비스명이 비어 있습니다.");
        }
    }
}
