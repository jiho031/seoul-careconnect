package com.seoulcareconnect.dto.user;

public record PolicyDocumentProgressResponse(
        Long policyId,
        Long policyDocumentId,
        boolean completed
) {
}
