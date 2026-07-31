package com.seoulcareconnect.config.policy;

import com.seoulcareconnect.service.policy.DocumentGuideService;
import com.seoulcareconnect.service.policy.PolicyDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class DocumentGuideStartupRunner implements ApplicationRunner {

    private final DocumentGuideService documentGuideService;
    private final PolicyDocumentService policyDocumentService;

    @Override
    public void run(ApplicationArguments args) {
        documentGuideService.seedDefaults();
        policyDocumentService.backfillMissingDocuments();
    }
}
