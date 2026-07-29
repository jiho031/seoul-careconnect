package com.seoulcareconnect.service.policy;

import com.seoulcareconnect.entity.policy.enums.SyncType;

import java.util.List;

public interface PolicyCollectService {

    PolicyCollectionSummary collectAll(SyncType syncType);

    void collectOne(String sourceName, SyncType syncType);

    List<String> availableSourceNames();
}