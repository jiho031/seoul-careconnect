package com.seoulcareconnect.integration.policy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.integration.policy.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "app.external.data-go.local-welfare.enabled",
        havingValue = "true"
)
public class LocalWelfareClient implements ExternalPolicyClient {

    private final ExternalApiSupport api;
    private final ExternalNodeReader reader;
    private final ExternalDateParser dateParser;
    private final ExternalPolicyClassifier classifier;
    private final String serviceKey;
    private final int pageNo;
    private final int numOfRows;
    private final int maxPages;
    private final int maxDetailCount;
    private final String listUrl;
    private final String detailUrl;

    public LocalWelfareClient(
            ExternalApiSupport api,
            ExternalNodeReader reader,
            ExternalDateParser dateParser,
            ExternalPolicyClassifier classifier,
            @Value("${app.external.data-go.service-key}") String serviceKey,
            @Value("${app.external.data-go.page-no:1}") int pageNo,
            @Value("${app.external.data-go.num-of-rows:100}") int numOfRows,
            @Value("${app.external.data-go.local-welfare.max-pages:1}") int maxPages,
            @Value("${app.external.data-go.local-welfare.max-detail-count:30}") int maxDetailCount,
            @Value("${app.external.data-go.local-welfare.list-url}") String listUrl,
            @Value("${app.external.data-go.local-welfare.detail-url}") String detailUrl
    ) {
        this.api = api;
        this.reader = reader;
        this.dateParser = dateParser;
        this.classifier = classifier;
        this.serviceKey = serviceKey;
        this.pageNo = Math.max(pageNo, 1);
        this.numOfRows = Math.min(Math.max(numOfRows, 1), 500);
        this.maxPages = Math.max(maxPages, 1);
        this.maxDetailCount = Math.max(maxDetailCount, 1);
        this.listUrl = listUrl;
        this.detailUrl = detailUrl;
    }

    @Override
    public String sourceName() {
        return "공공데이터포털-지자체복지서비스";
    }

    @Override
    public String sourceBaseUrl() {
        return listUrl;
    }

    @Override
    public String sourceCategory() {
        return "복지";
    }

    @Override
    public List<ExternalPolicyItem> fetch() {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("공공데이터포털 service-key가 비어 있습니다.");
        }

        Map<String, ExternalPolicyItem> result = new LinkedHashMap<>();
        int detailCalls = 0;

        for (int page = pageNo;
             page < pageNo + maxPages && detailCalls < maxDetailCount;
             page++) {

            // 지역 조건을 요청에 넣으면 NO DATA FOUND가 발생하므로 전국 목록을 먼저 조회합니다.
            String listRaw = api.get(listUrl, Map.of(
                    "serviceKey", serviceKey,
                    "pageNo", page,
                    "numOfRows", numOfRows
            ));

            JsonNode listRoot = api.readXml(listRaw);
            List<JsonNode> listItems =
                    reader.findObjectsContainingAny(listRoot, "servId");

            log.info(
                    "지자체 복지 API 목록 응답 확인: page={}, 응답길이={}, servId 발견수={}",
                    page,
                    listRaw == null ? 0 : listRaw.length(),
                    listItems.size()
            );

            if (listItems.isEmpty()) {
                String responsePreview = preview(listRaw, 1500);

                log.error(
                        "지자체 복지 API 목록에서 servId를 찾지 못했습니다. 응답 앞부분={}",
                        responsePreview
                );

                throw new IllegalStateException(
                        "지자체 복지 API 목록에서 servId를 찾지 못했습니다. "
                                + "API 응답 앞부분: "
                                + responsePreview
                );
            }

            for (JsonNode listItem : listItems) {
                if (detailCalls >= maxDetailCount) {
                    break;
                }

                String servId = reader.firstText(listItem, "servId");

                if (servId == null || result.containsKey(servId)) {
                    continue;
                }

                // 목록 응답에 지역이 있으면 서울 정책만 상세 API를 호출한다.
                String listRegion = reader.firstText(
                        listItem,
                        "ctpvNm",
                        "sidoNm",
                        "region"
                );

                if (listRegion != null && !isSeoulRegion(listRegion)) {
                    continue;
                }

                String detailRaw = null;
                JsonNode detailItem = null;

                try {
                    detailRaw = api.get(detailUrl, Map.of(
                            "serviceKey", serviceKey,
                            "servId", servId
                    ));

                    JsonNode detailRoot = api.readXml(detailRaw);
                    detailItem = reader.findObjectsContainingAny(detailRoot, "servId")
                            .stream()
                            .findFirst()
                            .orElse(detailRoot);

                } catch (RuntimeException detailError) {
                    log.warn(
                            "지자체 복지 상세 API 호출 실패: servId={}, 원인={}",
                            servId,
                            detailError.getMessage()
                    );
                } finally {
                    detailCalls++;
                }

                result.put(
                        servId,
                        mapItem(
                                listItem,
                                detailItem,
                                detailRaw == null ? listRaw : detailRaw
                        )
                );
            }

            if (listItems.size() < numOfRows) {
                break;
            }
        }

        log.info(
                "지자체 복지 API 변환 완료: 정책 후보수={}, 상세호출수={}",
                result.size(),
                detailCalls
        );

        return new ArrayList<>(result.values());
    }

    private ExternalPolicyItem mapItem(
            JsonNode list,
            JsonNode detail,
            String rawXml
    ) {
        String title = first(detail, list, "servNm", "serviceName", "title");

        String targetGroup = first(
                detail,
                list,
                "trgterIndvdlNmArray",
                "trgterIndvdlNm",
                "trgterIndvdl",
                "supportTarget",
                "target"
        );

        String supportTarget = first(
                detail,
                list,
                "sprtTrgtCn",
                "supportTargetContent"
        );

        String target = reader.joinNonBlank("\n", targetGroup, supportTarget);

        String summary = first(
                detail,
                list,
                "servDgst",
                "serviceSummary",
                "summary"
        );

        String benefit = first(
                detail,
                list,
                "alwServCn",
                "servDgst",
                "serviceSummary",
                "description"
        );

        String criteria = first(
                detail,
                list,
                "slctCritCn",
                "slctCrit",
                "selectionCriteria"
        );

        String applyMethod = first(
                detail,
                list,
                "aplyMtdCn",
                "aplyMtdNm",
                "aplyMtd",
                "applyMethod"
        );

        String officialUrl = first(
                detail,
                list,
                "servDtlLink",
                "servUrl",
                "homepageUrl",
                "link"
        );

        String agencyName = first(
                detail,
                list,
                "bizChrDeptNm",
                "jurOrgNm",
                "jurMnofNm",
                "organizationName",
                "agencyName"
        );

        String contact = first(
                detail,
                list,
                "rprsCtadr",
                "wlfareInfoReldCn",
                "phone",
                "contact"
        );

        // 신청 기간 필드만 사용하며, 시행 기간(enfcBgngYmd/enfcEndYmd)은 날짜로 저장하지 않습니다.
        String startText = first(
                detail,
                list,
                "aplyPrdBgngDt",
                "aplyBgngDt",
                "reqstBeginDe",
                "startDate"
        );

        String endText = first(
                detail,
                list,
                "aplyPrdEndDt",
                "aplyEndDt",
                "reqstEndDe",
                "endDate"
        );

        String periodText = first(
                detail,
                list,
                "aplyPrd",
                "aplyPrdCn",
                "reqstPd",
                "applicationPeriod"
        );

        ExternalDateParser.DateRange periodRange =
                dateParser.parseRange(periodText);

        var startDate = dateParser.parseSingle(startText);
        var endDate = dateParser.parseSingle(endText);

        if (startDate == null) {
            startDate = periodRange.startDate();
        }

        if (endDate == null) {
            endDate = periodRange.endDate();
        }

        String enforcementEndText = first(detail, list, "enfcEndYmd");
        boolean ongoingService = isOpenEndedEnforcement(enforcementEndText);

        String district = first(detail, list, "sggNm");

        String status = reader.joinNonBlank(
                " ",
                first(detail, list, "servStts", "serviceStatus", "status"),
                periodText,
                ongoingService ? "상시" : null
        );

        String documents = first(
                detail,
                list,
                "rqutDcmnt",
                "requiredDocuments",
                "reqstMthPapersCn"
        );

        String content = reader.joinNonBlank(
                "\n",
                targetGroup,
                supportTarget,
                criteria,
                benefit,
                applyMethod,
                documents,
                periodText
        );

        return ExternalPolicyItem.builder()
                .sourceName(sourceName())
                .sourceBaseUrl(sourceBaseUrl())
                .externalId(first(detail, list, "servId"))
                .title(reader.stripHtml(title))
                .agencyName(reader.stripHtml(agencyName))
                .summary(reader.stripHtml(summary))
                .category(classifier.category(
                        PolicyCategory.LIVING_SUPPORT,
                        title,
                        benefit,
                        target
                ))
                .target(reader.stripHtml(target))
                .region(first(detail, list, "ctpvNm"))
                .district(valueOr(
                        district,
                        classifier.district(title, target, benefit)
                ))
                .startDate(startDate)
                .endDate(endDate)
                .statusText(status)
                .applyMethod(reader.stripHtml(applyMethod))
                .officialUrl(officialUrl)
                .contact(reader.stripHtml(contact))
                .benefit(reader.stripHtml(benefit))
                .selectionCriteria(reader.stripHtml(criteria))
                .requiredDocumentsText(reader.stripHtml(documents))
                .contentText(reader.stripHtml(content))
                .rawXml(rawXml)
                .httpStatus(200)
                .build();
    }

    private boolean isOpenEndedEnforcement(String enforcementEndText) {
        if (enforcementEndText == null) {
            return false;
        }

        String digitsOnly = enforcementEndText.replaceAll("[^0-9]", "");
        return "99991231".equals(digitsOnly);
    }

    private String first(
            JsonNode preferred,
            JsonNode fallback,
            String... names
    ) {
        String value = reader.firstText(preferred, names);
        return value != null
                ? value
                : reader.firstText(fallback, names);
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank()
                ? fallback
                : value;
    }

    private String preview(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "응답 없음";
        }

        String normalized = value.replaceAll("\\s+", " ").trim();

        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength);
    }

    private boolean isSeoulRegion(String value) {
        if (value == null) {
            return false;
        }

        String normalized = value
                .replaceAll("\\s+", "")
                .trim();

        return "서울".equals(normalized)
                || "서울특별시".equals(normalized)
                || normalized.startsWith("서울특별시");
    }
}