package com.seoulcareconnect.dto.policy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDetailDTO {
    private Long policyId;
    private String title;
    private String agency;
    private String summary;
    private String sourceName;
    private String categoryLabel;
    private String ageGroupDisplay;
    private String target;
    private String regionDisplay;
    private String applyStatusLabel;
    private String dDayLabel;
    private String periodLabel;
    private String applyPeriod;
    private String applyMethod;
    private String officialUrl;
    private String contact;
    private String benefit;
    private String selectionCriteria;
    private String requiredDocumentsText;
    private String contentText;

    @Builder.Default
    private List<String> benefitLines = new ArrayList<>();

    @Builder.Default
    private List<String> documentLines = new ArrayList<>();

    @Builder.Default
    private List<String> applicationLines = new ArrayList<>();

    @Builder.Default
    private List<String> contentLines = new ArrayList<>();
}
