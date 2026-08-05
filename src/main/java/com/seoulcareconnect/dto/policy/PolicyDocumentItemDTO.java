package com.seoulcareconnect.dto.policy;

import com.seoulcareconnect.entity.policy.enums.DocumentCategory;
import com.seoulcareconnect.entity.policy.enums.DocumentRequirementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDocumentItemDTO {
    private Long policyDocumentId;
    private String checklistKey;
    private String originalText;
    private String displayName;
    private DocumentCategory category;
    private String categoryLabel;
    private DocumentRequirementType requirementType;
    private String requirementLabel;
    private String conditionText;
    private String alternativeGroup;
    private String attachmentName;
    private String downloadUrl;
    private String officialPageUrl;
    private Integer confidence;
    private boolean guideAvailable;
    private String guideTitle;
    private String issuer;
    private String officialGuideUrl;
    private String helpName;
    private String helpUrl;
    private String guideSummary;
    private String caution;
    private LocalDate verifiedOn;

    @Builder.Default
    private List<String> steps = new ArrayList<>();

    @Builder.Default
    private List<String> preparationItems = new ArrayList<>();
}
