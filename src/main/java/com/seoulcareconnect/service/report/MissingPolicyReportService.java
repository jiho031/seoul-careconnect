package com.seoulcareconnect.service.report;

import com.seoulcareconnect.dto.report.MissingPolicyReportCardDto;
import com.seoulcareconnect.dto.report.MissingPolicyReportRequestDto;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.PolicyCollectionError;
import com.seoulcareconnect.entity.policy.enums.PolicyErrorStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyErrorType;
import com.seoulcareconnect.entity.report.MissingPolicyReport;
import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.policy.PolicyCollectionErrorRepository;
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
    private final PolicyCollectionErrorRepository policyCollectionErrorRepository;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;

    /**
     * 사용자 신고 등록
     *
     * 1. missing_policy_reports에 사용자 신고 저장
     * 2. 정책 오류 관리 대상이면 policy_collection_errors에도 저장
     */
    @Transactional
    public Long submitReport(
            MissingPolicyReportRequestDto requestDto
    ) {
        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "로그인이 필요합니다."
                        )
                );

        Policy policy = findPolicy(requestDto.getPolicyId());

        MissingPolicyReport report = new MissingPolicyReport();
        report.setUser(user);
        report.setPolicy(policy);
        report.setTitle(requestDto.getTitle());
        report.setSourceUrl(requestDto.getSourceUrl());
        report.setReportType(requestDto.getReportType());
        report.setContent(buildContent(requestDto));

        /*
         * saveAndFlush를 사용해 reportId 생성과 DB 저장을
         * 정책 오류 연동 전에 바로 확인한다.
         */
        MissingPolicyReport savedReport =
                missingPolicyReportRepository.saveAndFlush(report);

        createPolicyCollectionError(savedReport);

        return savedReport.getReportId();
    }

    /**
     * policyId가 전달된 경우 신고 대상 정책 조회
     */
    private Policy findPolicy(Long policyId) {
        if (policyId == null) {
            return null;
        }

        return policyRepository.findById(policyId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "신고 대상 정책을 찾을 수 없습니다."
                        )
                );
    }

    /**
     * 사용자 신고를 정책 오류 관리 항목으로 변환해 저장
     */
    private void createPolicyCollectionError(
            MissingPolicyReport report
    ) {
        System.out.println(
                "========== 정책 오류 연동 시작 =========="
        );
        System.out.println(
                "reportId = " + report.getReportId()
        );
        System.out.println(
                "reportType = [" + report.getReportType() + "]"
        );

        PolicyErrorType errorType =
                convertReportType(report.getReportType());

        System.out.println(
                "변환된 errorType = " + errorType
        );

        if (errorType == null) {
            System.out.println(
                    "중단 사유: 지원하지 않는 reportType"
            );
            return;
        }

        Policy policy = report.getPolicy();
        boolean missingReport =
                isMissingReport(report.getReportType());

        System.out.println(
                "누락 정책 신고 여부 = " + missingReport
        );

        if (policy != null) {
            System.out.println(
                    "policyId = " + policy.getPolicyId()
            );
            System.out.println(
                    "policyTitle = " + policy.getTitle()
            );
            System.out.println(
                    "source = " + policy.getSource()
            );
        } else {
            System.out.println("연결된 policy 없음");
        }

        /*
         * 정보 오류, 마감일 오류, 링크 오류는 기존 정책 신고이므로
         * policy와 source가 모두 있어야 한다.
         *
         * 누락 정책 신고는 DB에 해당 정책이 없을 수 있으므로
         * policy와 source 없이도 저장한다.
         */
        if (!missingReport
                && (policy == null
                || policy.getSource() == null)) {

            System.out.println(
                    "중단 사유: 기존 정책 또는 source 없음"
            );
            return;
        }

        PolicyCollectionError error =
                new PolicyCollectionError();

        if (policy != null) {
            error.setPolicy(policy);
            error.setRawItem(policy.getRawItem());
            error.setSource(policy.getSource());
            error.setExternalId(policy.getExternalId());
            error.setPolicyTitle(policy.getTitle());
        } else {
            /*
             * 누락 정책 신고는 사용자 입력값 사용
             */
            error.setPolicy(null);
            error.setRawItem(null);
            error.setSource(null);
            error.setExternalId(report.getSourceUrl());
            error.setPolicyTitle(
                    resolveMissingPolicyTitle(report)
            );
        }

        error.setErrorType(errorType);
        error.setErrorMessage(
                "[사용자 신고 #"
                        + report.getReportId()
                        + "]\n"
                        + report.getContent()
        );
        error.setStatus(PolicyErrorStatus.WAITING);

        System.out.println(
                "PolicyCollectionError 저장 직전"
        );

        PolicyCollectionError savedError =
                policyCollectionErrorRepository
                        .saveAndFlush(error);

        System.out.println(
                "정책 오류 저장 완료: errorId = "
                        + savedError.getErrorId()
        );
        System.out.println(
                "========== 정책 오류 연동 종료 =========="
        );
    }

    /**
     * 누락 정책의 표시 제목 결정
     */
    private String resolveMissingPolicyTitle(
            MissingPolicyReport report
    ) {
        if (report.getTitle() != null
                && !report.getTitle().isBlank()) {
            return report.getTitle();
        }

        return "사용자 누락 정책 신고";
    }

    /**
     * 사용자 신고 유형 → 정책 오류 유형 변환
     */
    private PolicyErrorType convertReportType(
            String reportType
    ) {
        if (reportType == null
                || reportType.isBlank()) {
            return null;
        }

        return switch (reportType.trim()) {
            case "MISSING", "MISSING_POLICY" ->
                    PolicyErrorType.REQUIRED_VALUE_MISSING;

            case "INFO_ERROR" ->
                    PolicyErrorType.FORMAT_ERROR;

            case "DEADLINE_ERROR" ->
                    PolicyErrorType.DATE_ERROR;

            case "LINK_ERROR" ->
                    PolicyErrorType.URL_ERROR;

            default ->
                    null;
        };
    }

    /**
     * 누락 정책 신고 여부 확인
     */
    private boolean isMissingReport(
            String reportType
    ) {
        if (reportType == null) {
            return false;
        }

        String normalizedType =
                reportType.trim();

        return "MISSING".equals(normalizedType)
                || "MISSING_POLICY".equals(normalizedType);
    }

    /**
     * 사용자 입력 내용을 신고 상세 내용으로 구성
     */
    private String buildContent(
            MissingPolicyReportRequestDto requestDto
    ) {
        StringBuilder sb = new StringBuilder();

        if (requestDto.getPolicyName() != null
                && !requestDto.getPolicyName().isBlank()) {

            sb.append("[정책명] ")
                    .append(requestDto.getPolicyName())
                    .append("\n");
        }

        if (requestDto.getRegion() != null
                && !requestDto.getRegion().isBlank()) {

            sb.append("[지역] ")
                    .append(requestDto.getRegion())
                    .append("\n");
        }

        if (requestDto.getContent() != null
                && !requestDto.getContent().isBlank()) {

            sb.append(requestDto.getContent());
        }

        return sb.toString();
    }

    /**
     * 마이페이지 신고 내역 카드 목록
     */
    public List<MissingPolicyReportCardDto> getMyReports(
            Long userId
    ) {
        return missingPolicyReportRepository
                .findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toCardDto)
                .collect(Collectors.toList());
    }

    private MissingPolicyReportCardDto toCardDto(
            MissingPolicyReport report
    ) {
        return MissingPolicyReportCardDto.builder()
                .reportId(report.getReportId())
                .title(report.getTitle())
                .reportType(report.getReportType())
                .reportTypeLabel(
                        reportTypeLabel(
                                report.getReportType()
                        )
                )
                .status(report.getStatus())
                .statusLabel(
                        statusLabel(report.getStatus())
                )
                .policyTitle(
                        report.getPolicy() != null
                                ? report.getPolicy().getTitle()
                                : null
                )
                .createdAt(report.getCreatedAt())
                .build();
    }

    private String reportTypeLabel(
            String reportType
    ) {
        if (reportType == null) {
            return "미분류";
        }

        return switch (reportType.trim()) {
            case "MISSING", "MISSING_POLICY" ->
                    "누락 정책";

            case "INFO_ERROR" ->
                    "정보 오류";

            case "DEADLINE_ERROR" ->
                    "마감일 오류";

            case "LINK_ERROR" ->
                    "링크 오류";

            default ->
                    "미분류";
        };
    }

    private String statusLabel(
            String status
    ) {
        if (status == null) {
            return "접수";
        }

        return switch (status) {
            case "RECEIVED" ->
                    "접수";

            case "IN_REVIEW" ->
                    "확인 중";

            case "COMPLETED" ->
                    "반영 완료";

            case "REJECTED" ->
                    "처리 제외";

            default ->
                    "접수";
        };
    }
}