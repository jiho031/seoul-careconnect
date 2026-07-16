package com.seoulcareconnect.repository.admin;

import com.seoulcareconnect.entity.admin.AdminActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminActivityLogRepository
        extends JpaRepository<AdminActivityLog, Long> {

    List<AdminActivityLog>
    findTop5ByOrderByCreatedAtDesc();

    List<AdminActivityLog>
    findAllByCreatedAtAfterOrderByCreatedAtDesc(
            LocalDateTime cutoffDateTime
    );

    long deleteByCreatedAtBefore(
            LocalDateTime cutoffDateTime
    );
}