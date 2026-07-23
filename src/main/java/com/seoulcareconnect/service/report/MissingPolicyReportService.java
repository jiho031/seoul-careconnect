package com.seoulcareconnect.service.report;

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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissingPolicyReportService {

    private final MissingPolicyReportRepository missingPolicyReportRepository;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;

    @Transactional
    public Long submitReport(MissingPolicyReportRequestDto requestDto) {
        // userId는 클라이언트(hidden input)에서 넘어오는 값이라 위조 가능성이 있어
        // 실제 존재하는 회원인지 조회로 검증합니다.
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

    // 엔티티에 policyName/region 전용 컬럼이 없어서 상세 설명 앞에 붙여서 저장합니다.
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

    // 마이페이지 등에서 "내가 신고한 내역" 보여줄 때 사용 (필요 시 활용)
    public List<MissingPolicyReport> getMyReports(Long userId) {
        return missingPolicyReportRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
    }
}
