package com.seoulcareconnect.repository.admin;

import com.seoulcareconnect.entity.admin.AdminNotice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminNoticeRepository
        extends JpaRepository<AdminNotice, Long> {
}