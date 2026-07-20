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
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "app.external.data-go.central-welfare.enabled",
        havingValue = "true"
)
public class CentralWelfareClient implements ExternalPolicyClient {

    private final ExternalApiSupport api;
    private final ExternalNodeReader reader;
    private final ExternalDateParser dateParser;
    private final ExternalPolicyClassifier classifier;

    private final String serviceKey;
    private final String listUrl;
    private final String detailUrl;
    private final String listCallType;
    private final String detailCallType;

    private final int pageNo;
    private final int maxDetailCount;

    public CentralWelfareClient(
            ExternalApiSupport api,
            ExternalNodeReader reader,
            ExternalDateParser dateParser,
            ExternalPolicyClassifier classifier,

            @Value("${app.external.data-go.service-key}")
            String serviceKey,

            @Value("${app.external.data-go.central-welfare.list-url}")
            String listUrl,

            @Value("${app.external.data-go.central-welfare.detail-url}")
            String detailUrl,

            @Value("${app.external.data-go.central-welfare.list-call-type:L}")
            String listCallType,

            @Value("${app.external.data-go.central-welfare.detail-call-type:D}")
            String detailCallType,

            @Value("${app.external.data-go.page-no:1}")
            int pageNo,

            @Value("${app.external.data-go.central-welfare.max-detail-count:40}")
            int maxDetailCount
    ) {
        this.api = api;
        this.reader = reader;
        this.dateParser = dateParser;
        this.classifier = classifier;

        this.serviceKey = serviceKey;
        this.listUrl = listUrl;
        this.detailUrl = detailUrl;
        this.listCallType = listCallType;
        this.detailCallType = detailCallType;

        this.pageNo = Math.max(pageNo, 1);
        this.maxDetailCount =
                Math.min(Math.max(maxDetailCount, 1), 90);
    }

    @Override
    public String sourceName() {
        return "공공데이터포털-중앙부처복지서비스";
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

        String listRaw = api.get(
                listUrl,
                Map.of(
                        "serviceKey", serviceKey,
                        "callTp", listCallType,
                        "pageNo", pageNo,
                        "numOfRows", maxDetailCount
                )
        );

        JsonNode root = api.readXml(listRaw);

        List<JsonNode> listItems =
                reader.findObjectsContainingAny(
                        root,
                        "servId"
                );

        if (listItems.isEmpty()) {
            String preview = listRaw == null
                    ? "(응답 없음)"
                    : listRaw
                    .replaceAll("\\s+", " ")
                    .substring(
                            0,
                            Math.min(
                                    listRaw.replaceAll("\\s+", " ").length(),
                                    1000
                            )
                    );

            throw new IllegalStateException(
                    "중앙부처 복지 목록 응답에서 servId를 찾지 못했습니다. "
                            + "응답 앞부분="
                            + preview
            );
        }

        log.info(
                "중앙부처 복지 API 목록 응답 확인: "
                        + "servId 발견수={}",
                listItems.size()
        );

        List<ExternalPolicyItem> result =
                new ArrayList<>();

        int detailCalls = 0;

        for (JsonNode listItem :
                listItems.stream()
                        .limit(maxDetailCount)
                        .toList()) {

            String servId =
                    reader.firstText(
                            listItem,
                            "servId"
                    );

            if (servId == null
                    || servId.isBlank()) {
                continue;
            }

            String detailRaw = null;
            JsonNode detailItem = null;

            try {
                detailRaw = api.get(
                        detailUrl,
                        Map.of(
                                "serviceKey", serviceKey,
                                "callTp", detailCallType,
                                "servId", servId
                        )
                );

                JsonNode detailRoot =
                        api.readXml(detailRaw);

                detailItem =
                        reader.findObjectsContainingAny(
                                        detailRoot,
                                        "servId"
                                )
                                .stream()
                                .findFirst()
                                .orElse(detailRoot);

            } catch (RuntimeException e) {
                log.warn(
                        "중앙부처 복지 상세 API 호출 실패: "
                                + "servId={}, 원인={}",
                        servId,
                        e.getMessage()
                );

            } finally {
                detailCalls++;

                log.info(
                        "중앙부처 복지 상세 API 진행: "
                                + "{}/{}건, servId={}",
                        detailCalls,
                        maxDetailCount,
                        servId
                );
            }

            ExternalPolicyItem item =
                    mapItem(
                            listItem,
                            detailItem,
                            detailRaw == null
                                    ? listRaw
                                    : detailRaw
                    );

            result.add(item);
        }

        log.info(
                "중앙부처 복지 API 변환 완료: "
                        + "정책 후보수={}, 상세호출수={}",
                result.size(),
                detailCalls
        );

        return result;
    }

    private ExternalPolicyItem mapItem(
            JsonNode listItem,
            JsonNode detailItem,
            String rawXml
    ) {
        String title = first(
                detailItem,
                listItem,
                "servNm",
                "serviceName",
                "title"
        );

        String summary = first(
                detailItem,
                listItem,
                "servDgst",
                "serviceSummary",
                "summary",
                "description"
        );

        String benefit = first(
                detailItem,
                listItem,
                "alwServCn",
                "serviceContent",
                "benefit"
        );

        String target = first(
                detailItem,
                listItem,
                "trgterIndvdlNm",
                "trgterIndvdl",
                "supportTarget",
                "target"
        );

        String criteria = first(
                detailItem,
                listItem,
                "slctCrit",
                "slctCritCn",
                "selectionCriteria"
        );

        String applyMethod = first(
                detailItem,
                listItem,
                "aplyMtd",
                "aplyMtdCn",
                "applyMethod"
        );

        String officialUrl = first(
                detailItem,
                listItem,
                "servDtlLink",
                "servUrl",
                "homepageUrl",
                "link"
        );

        String agencyName = first(
                detailItem,
                listItem,
                "jurOrgNm",
                "jurMnofNm",
                "organizationName",
                "agencyName"
        );

        String phone = first(
                detailItem,
                listItem,
                "rprsCtadr",
                "phone",
                "contact"
        );

        String contact =
                reader.joinNonBlank(
                        " / ",
                        agencyName,
                        phone
                );

        String dateRange = first(
                detailItem,
                listItem,
                "aplyPrd",
                "reqstBeginEndDe",
                "applicationPeriod"
        );

        ExternalDateParser.DateRange range =
                dateParser.parseRange(dateRange);

        String startText = first(
                detailItem,
                listItem,
                "aplyPrdBgngDt",
                "aplyBgngDt",
                "reqstBeginDe",
                "startDate"
        );

        String endText = first(
                detailItem,
                listItem,
                "aplyPrdEndDt",
                "aplyEndDt",
                "reqstEndDe",
                "endDate"
        );

        String status = first(
                detailItem,
                listItem,
                "servStts",
                "serviceStatus",
                "status",
                "onapPsbltYn"
        );

        String documents = first(
                detailItem,
                listItem,
                "rqutDcmnt",
                "requiredDocuments",
                "reqstMthPapersCn"
        );

        /*
         * 중앙부처 응답에 지역 정보가 명확히 있을 때만 넣습니다.
         *
         * 지역값이 null이면 SeoulPolicyFilter에서
         * 중앙부처 정책을 전국 정책으로 판단합니다.
         */
        String region = first(
                detailItem,
                listItem,
                "ctpvNm",
                "sidoNm",
                "region",
                "regionNm",
                "areaNm"
        );

        String content = reader.joinNonBlank(
                "\n",
                summary,
                target,
                criteria,
                benefit,
                applyMethod,
                documents
        );

        return ExternalPolicyItem.builder()
                .sourceName(sourceName())
                .sourceBaseUrl(sourceBaseUrl())

                .externalId(first(
                        detailItem,
                        listItem,
                        "servId"
                ))

                .title(reader.stripHtml(title))
                .agencyName(
                        reader.stripHtml(agencyName)
                )
                .summary(
                        reader.stripHtml(summary)
                )

                .category(
                        classifier.category(
                                PolicyCategory.CULTURE_LIFE,
                                title,
                                summary,
                                benefit,
                                target
                        )
                )

                .target(reader.stripHtml(target))
                .region(reader.stripHtml(region))

                .district(
                        classifier.district(
                                region,
                                title,
                                target,
                                summary,
                                benefit
                        )
                )

                .startDate(
                        range.startDate() != null
                                ? range.startDate()
                                : dateParser.parseSingle(
                                startText
                        )
                )

                .endDate(
                        range.endDate() != null
                                ? range.endDate()
                                : dateParser.parseSingle(
                                endText
                        )
                )

                .statusText(status)
                .applyMethod(
                        reader.stripHtml(applyMethod)
                )
                .officialUrl(officialUrl)
                .contact(
                        reader.stripHtml(contact)
                )
                .benefit(
                        reader.stripHtml(benefit)
                )
                .selectionCriteria(
                        reader.stripHtml(criteria)
                )
                .requiredDocumentsText(
                        reader.stripHtml(documents)
                )
                .contentText(
                        reader.stripHtml(content)
                )

                .applicationInfoAvailable(false)

                .rawXml(rawXml)
                .httpStatus(200)
                .build();
    }

    private String first(
            JsonNode preferred,
            JsonNode fallback,
            String... names
    ) {
        String value =
                reader.firstText(
                        preferred,
                        names
                );

        return value != null
                ? value
                : reader.firstText(
                fallback,
                names
        );
    }
}
