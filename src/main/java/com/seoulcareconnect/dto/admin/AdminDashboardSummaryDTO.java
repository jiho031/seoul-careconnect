package com.seoulcareconnect.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminDashboardSummaryDTO {

    private long totalPolicyCount;

    private long totalSourceCount;
    private long activeSourceCount;

    private long latestCollectionSuccessCount;
    private long latestCollectionFailCount;

    private long totalUserCount;
    private long activeUserCount;
}