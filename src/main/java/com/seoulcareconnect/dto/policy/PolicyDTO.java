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
public class PolicyDTO {
    private Long policyId;
    private String title;
    private String agency;
    private String summary;
    private String category;
    private String categoryLabel;
    private String target;
    private String ageGroupDisplay;
    private String regionDisplay;
    private String applyStatus;
    private String applyStatusLabel;
    private String applyPeriod;
    private String applyMethod;
    private String dDayLabel;
    private String dDayCssClass;
    private String createdDate;
    private Integer viewCount;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    private boolean newPolicy;
}
