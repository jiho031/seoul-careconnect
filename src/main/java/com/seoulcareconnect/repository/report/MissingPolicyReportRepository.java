package com.seoulcareconnect.repository.report;

import com.seoulcareconnect.entity.report.MissingPolicyReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface MissingPolicyReportRepository
        extends JpaRepository<MissingPolicyReport, Long>,
        JpaSpecificationExecutor<MissingPolicyReport> {

    // 마이페이지에서 내가 신고한 내역 조회
    // User 엔티티의 PK 필명이 userId가 아니면 실제 필드명에 맞게 변경
    List<MissingPolicyReport> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    // 관리자 페이지 전체 목록
    Page<MissingPolicyReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 관리자 페이지 상태별 필터링
    Page<MissingPolicyReport> findByStatusOrderByCreatedAtDesc(
            String status,
            Pageable pageable
    );

    // 관리자 통계
    long countByStatus(String status);

    long countByReportTypeAndStatusNot(
            String reportType,
            String excludedStatus
    );
}