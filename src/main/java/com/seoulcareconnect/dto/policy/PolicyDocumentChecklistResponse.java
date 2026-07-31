package com.seoulcareconnect.dto.policy;

import java.util.List;
import java.util.Set;

public record PolicyDocumentChecklistResponse(
        Long policyId,
        String policyTitle,
        List<PolicyDocumentItemDTO> documents,
        Set<String> completedDocumentKeys
) {
}
