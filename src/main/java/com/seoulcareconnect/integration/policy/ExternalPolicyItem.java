package com.seoulcareconnect.integration.policy;

import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalPolicyItem {
    private String sourceName;
    private String sourceBaseUrl;
    private String externalId;
    private String title;
    private String agencyName;
    private String summary;
    private PolicyCategory category;
    private String target;
    private String region;
    private String district;
    private LocalDate startDate;
    private LocalDate endDate;
    private String statusText;
    private String applyMethod;
    private String officialUrl;
    private String contact;
    private String benefit;
    private String selectionCriteria;
    private String requiredDocumentsText;
    private String contentText;
    private String rawJson;
    private String rawXml;
    private Integer httpStatus;

    @Builder.Default
    private boolean applicationInfoAvailable = true;
}
