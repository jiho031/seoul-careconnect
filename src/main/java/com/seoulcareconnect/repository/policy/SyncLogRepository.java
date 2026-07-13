package com.seoulcareconnect.repository.policy;

import com.seoulcareconnect.entity.policy.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {
}