package com.seoulcareconnect.integration.policy;

import java.util.List;

public interface ExternalPolicyClient {
    String sourceName();
    String sourceBaseUrl();
    String sourceCategory();
    List<ExternalPolicyItem> fetch();
}