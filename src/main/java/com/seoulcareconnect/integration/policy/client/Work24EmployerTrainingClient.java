package com.seoulcareconnect.integration.policy.client;

import com.seoulcareconnect.integration.policy.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.external.work24.employer-training.enabled", havingValue = "true")
public class Work24EmployerTrainingClient extends AbstractWork24TrainingClient {

    public Work24EmployerTrainingClient(
            ExternalApiSupport api,
            ExternalNodeReader reader,
            ExternalDateParser dateParser,
            ExternalPolicyClassifier classifier,
            @Value("${app.external.work24.employer-training.auth-key}") String authKey,
            @Value("${app.external.work24.return-type:XML}") String returnType,
            @Value("${app.external.work24.employer-training.url}") String url,
            @Value("${app.external.work24.employer-training.out-type:1}") String outType,
            @Value("${app.external.work24.employer-training.page-num:1}") int pageNum,
            @Value("${app.external.work24.employer-training.page-size:100}") int pageSize,
            @Value("${app.external.work24.employer-training.area-code:11}") String areaCode,
            @Value("${app.external.work24.employer-training.sort:ASC}") String sort,
            @Value("${app.external.work24.employer-training.sort-column:2}") String sortColumn,
            @Value("${app.external.work24.employer-training.max-pages:5}") int maxPages,
            @Value("${app.policy.collect.lookback-days:365}") int lookbackDays,
            @Value("${app.policy.collect.future-months:12}") int futureMonths,
            @Value("${app.policy.sync.zone-id:Asia/Seoul}") String zoneId
    ) {
        super(api, reader, dateParser, classifier, authKey, returnType, url, outType,
                pageNum, pageSize, areaCode, sort, sortColumn, maxPages,
                lookbackDays, futureMonths, zoneId);
    }

    @Override
    public String sourceName() {
        return "고용24-사업주훈련";
    }
}