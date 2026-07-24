package com.seoulcareconnect.repository.report;

import com.seoulcareconnect.entity.report.MissingPolicyReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissingPolicyReportRepository extends JpaRepository<MissingPolicyReport, Long> {


    List<MissingPolicyReport> findByUser_UserIdOrderByCreatedAtDesc(Long userId);


    Page<MissingPolicyReport> findAllByOrderByCreatedAtDesc(Pageable pageable);


    Page<MissingPolicyReport> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
