package com.seoulcareconnect.repository.report;

import com.seoulcareconnect.entity.report.MissingPolicyReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissingPolicyReportRepository extends JpaRepository<MissingPolicyReport, Long> {

    // 마이페이지 등에서 "내가 신고한 내역"을 보여줄 때 사용
    // User 엔티티의 PK 필드명이 userId가 아니라면 이 메서드명을 그에 맞게 바꿔주세요.
    List<MissingPolicyReport> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    // 관리자 페이지 전체 목록용 (jiho031님 관리자 페이지에서 사용 가능)
    Page<MissingPolicyReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 관리자 페이지 상태별 필터링용
    Page<MissingPolicyReport> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
