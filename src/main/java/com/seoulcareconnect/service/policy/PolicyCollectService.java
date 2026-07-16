package com.seoulcareconnect.service.policy;

import com.seoulcareconnect.entity.policy.enums.SyncType;

public interface PolicyCollectService {
    void collectAll(SyncType syncType);
}