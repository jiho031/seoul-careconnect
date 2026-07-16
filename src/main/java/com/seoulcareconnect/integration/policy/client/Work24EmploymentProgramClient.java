package com.seoulcareconnect.integration.policy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.integration.policy.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.external.work24.employment-program.enabled", havingValue = "true")
public class Work24EmploymentProgramClient implements ExternalPolicyClient {

    private final ExternalApiSupport api;
    private final ExternalNodeReader reader;
    private final ExternalDateParser dateParser;
    private final ExternalPolicyClassifier classifier;
    private final String authKey;
    private final String returnType;
    private final String url;
    private final int startPage;
    private final int display;
    private final int maxPages;
    private final String zoneId;

    public Work24EmploymentProgramClient(
            ExternalApiSupport api,
            ExternalNodeReader reader,
            ExternalDateParser dateParser,
            ExternalPolicyClassifier classifier,
            @Value("${app.external.work24.employment-program.auth-key}") String authKey,
            @Value("${app.external.work24.return-type:XML}") String returnType,
            @Value("${app.external.work24.employment-program.url}") String url,
            @Value("${app.external.work24.employment-program.start-page:1}") int startPage,
            @Value("${app.external.work24.employment-program.display:100}") int display,
            @Value("${app.external.work24.employment-program.max-pages:5}") int maxPages,
            @Value("${app.policy.sync.zone-id:Asia/Seoul}") String zoneId
    ) {
        this.api = api;
        this.reader = reader;
        this.dateParser = dateParser;
        this.classifier = classifier;
        this.authKey = authKey;
        this.returnType = returnType;
        this.url = url;
        this.startPage = Math.max(startPage, 1);
        this.display = Math.min(Math.max(display, 1), 100);
        this.maxPages = Math.max(maxPages, 1);
        this.zoneId = zoneId;
    }

    @Override public String sourceName() { return "고용24-구직자취업역량강화프로그램"; }
    @Override public String sourceBaseUrl() { return url; }
    @Override public String sourceCategory() { return "취업·교육"; }

    @Override
    public List<ExternalPolicyItem> fetch() {
        List<ExternalPolicyItem> result = new ArrayList<>();
        String today = LocalDate.now(ZoneId.of(zoneId)).format(DateTimeFormatter.BASIC_ISO_DATE);

        for (int page = startPage; page < startPage + maxPages; page++) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("authKey", authKey);
            params.put("returnType", returnType);
            params.put("startPage", page);
            params.put("display", display);
            params.put("pgmStdt", today);

            String raw = api.get(url, params);
            JsonNode root = api.readXml(raw);
            List<JsonNode> items = reader.findObjectsContainingAny(root, "pgmNm");

            if (items.isEmpty()) break;
            items.forEach(item -> result.add(mapItem(item, raw)));
            if (items.size() < display) break;
        }
        return result;
    }

    private ExternalPolicyItem mapItem(JsonNode item, String rawXml) {
        String program = reader.firstText(item, "pgmNm");
        String course = reader.firstText(item, "pgmSubNm");
        String target = reader.firstText(item, "pgmTarget");
        String place = reader.firstText(item, "openPlcCont");
        String agencyName = reader.firstText(item, "orgNm");

        String time = reader.joinNonBlank(" / ",
                reader.firstText(item, "openTime"),
                reader.firstText(item, "operationTime")
        );

        String summary = reader.joinNonBlank("\n", target, place, time);
        String region = place;

        if (place != null && (place.contains("온라인") || place.contains("비대면"))) {
            region = "전국";
        }

        return ExternalPolicyItem.builder()
                .sourceName(sourceName()).sourceBaseUrl(sourceBaseUrl())
                .externalId(reader.firstText(item, "pgmId", "pgmSeq", "seq"))
                .title(reader.stripHtml(reader.joinNonBlank(" - ", program, course)))
                .agencyName(reader.stripHtml(agencyName))
                .summary(reader.stripHtml(summary))
                .category(classifier.category(PolicyCategory.JOB, program, course, target))
                .target(reader.stripHtml(target))
                .region(reader.stripHtml(region))
                .district(classifier.district(place))
                .startDate(dateParser.parseSingle(reader.firstText(item, "pgmStdt")))
                .endDate(dateParser.parseSingle(reader.firstText(item, "pgmEndt")))
                .statusText(reader.firstText(item, "status", "pgmSttus"))
                .applyMethod("고용센터 프로그램 신청 안내 확인")
                .officialUrl(reader.firstText(item, "url", "link", "detailUrl"))
                .contact(reader.stripHtml(agencyName))
                .benefit(reader.stripHtml(summary))
                .contentText(reader.stripHtml(summary))
                .applicationInfoAvailable(false)
                .rawXml(rawXml).httpStatus(200)
                .build();
    }
}
