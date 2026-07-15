package com.seoulcareconnect.integration.policy.client;

import com.seoulcareconnect.integration.policy.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.external.data-go.myhome.enabled", havingValue = "true")
public class MyHomeSaleClient extends AbstractMyHomeClient {

    public MyHomeSaleClient(
            ExternalApiSupport api,
            ExternalNodeReader reader,
            ExternalDateParser dateParser,
            ExternalPolicyClassifier classifier,
            @Value("${app.external.data-go.service-key}") String serviceKey,
            @Value("${app.external.data-go.myhome.sale-url}") String url,
            @Value("${app.external.data-go.myhome.region-code:11}") String regionCode,
            @Value("${app.external.data-go.page-no:1}") int pageNo,
            @Value("${app.external.data-go.num-of-rows:100}") int numOfRows,
            @Value("${app.external.data-go.myhome.max-pages:5}") int maxPages
    ) {
        super(api, reader, dateParser, classifier,
                serviceKey, url, regionCode, pageNo, numOfRows, maxPages);
    }

    @Override
    public String sourceName() {
        return "마이홈-공공분양주택모집공고";
    }
}