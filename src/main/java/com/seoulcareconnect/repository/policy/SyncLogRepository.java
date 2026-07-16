package com.seoulcareconnect.repository.policy;

import com.seoulcareconnect.entity.policy.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {

    List<SyncLog> findTop13ByOrderByStartedAtDesc();

    long deleteByStartedAtBefore(
            LocalDateTime cutoffDateTime
    );
}