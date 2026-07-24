// 실제 프로젝트 경로: src/main/java/com/seoulcareconnect/service/report/MissingPolicyReportService.java
// [수정] getMyReports()가 엔티티를 그대로 반환하던 것을, 마이페이지 카드 표시용
// MissingPolicyReportCardDto 리스트를 반환하도록 변경했습니다. (FavoriteService.getFavoriteCards()와 동일한 패턴)
package com.seoulcareconnect.service.report;

import com.seoulcareconnect.dto.report.MissingPolicyReportCardDto;
import com.seoulcareconnect.dto.report.MissingPolicyReportRequestDto;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.report.MissingPolicyReport;
import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import com.seoulcareconnect.repository.report.MissingPolicyReportRepository;
import com.seoulcareconnect.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissingPolicyReportService {

    private final MissingPolicyReportRepository missingPolicyReportRepository;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    @Transactional
    public Long submitReport(MissingPolicyReportRequestDto requestDto) {

        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));

        Policy policy = null;
        if (requestDto.getPolicyId() != null) {
            policy = policyRepository.findById(requestDto.getPolicyId()).orElse(null);
        }

        MissingPolicyReport report = new MissingPolicyReport();
        report.setUser(user);
        report.setPolicy(policy);
        report.setTitle(requestDto.getTitle());
        report.setSourceUrl(requestDto.getSourceUrl());
        report.setReportType(requestDto.getReportType());
        report.setContent(buildContent(requestDto));

        return missingPolicyReportRepository.save(report).getReportId();
    }


    private String buildContent(MissingPolicyReportRequestDto requestDto) {
        StringBuilder sb = new StringBuilder();
        if (requestDto.getPolicyName() != null && !requestDto.getPolicyName().isBlank()) {
            sb.append("[정책명] ").append(requestDto.getPolicyName()).append("\n");
        }
        if (requestDto.getRegion() != null && !requestDto.getRegion().isBlank()) {
            sb.append("[지역] ").append(requestDto.getRegion()).append("\n");
        }
        sb.append(requestDto.getContent());
        return sb.toString();
    }

    // 마이페이지 "신고 내역" 카드 목록용
    public List<MissingPolicyReportCardDto> getMyReports(Long userId) {
        return missingPolicyReportRepository.findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toCardDto)
                .collect(Collectors.toList());
    }

    private MissingPolicyReportCardDto toCardDto(MissingPolicyReport report) {
        return MissingPolicyReportCardDto.builder()
                .reportId(report.getReportId())
                .title(report.getTitle())
                .reportType(report.getReportType())
                .reportTypeLabel(reportTypeLabel(report.getReportType()))
                .status(report.getStatus())
                .statusLabel(statusLabel(report.getStatus()))
                .policyTitle(report.getPolicy() != null ? report.getPolicy().getTitle() : null)
                .createdAt(report.getCreatedAt())
                .build();
    }

    private String reportTypeLabel(String reportType) {
        if (reportType == null) {
            return "미분류";
        }
        return switch (reportType) {
            case "MISSING" -> "누락 정책";
            case "INFO_ERROR" -> "정보 오류";
            case "DEADLINE_ERROR" -> "마감일 오류";
            case "LINK_ERROR" -> "링크 오류";
            default -> "미분류";
        };
    }

    private String statusLabel(String status) {
        if (status == null) {
            return "접수";
        }
        return switch (status) {
            case "RECEIVED" -> "접수";
            case "IN_REVIEW" -> "확인 중";
            case "COMPLETED" -> "반영 완료";
            default -> "접수";
        };
    }
}
