package com.seoulcareconnect.service.admin;

import com.seoulcareconnect.dto.admin.AdminPolicyFormDTO;
import com.seoulcareconnect.entity.admin.enums.AdminActivityType;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.PolicyDetail;
import com.seoulcareconnect.entity.policy.PolicySource;
import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.entity.policy.enums.PolicyStatus;
import com.seoulcareconnect.entity.policy.enums.SourceType;
import com.seoulcareconnect.entity.report.MissingPolicyReport;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import com.seoulcareconnect.repository.policy.PolicySourceRepository;
import com.seoulcareconnect.repository.report.MissingPolicyReportRepository;
import com.seoulcareconnect.service.ai.AiSummaryAutomationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPolicyFormService {

    private final PolicyRepository policyRepository;
    private final PolicySourceRepository policySourceRepository;
    private final MissingPolicyReportRepository
            missingPolicyReportRepository;
    private final AdminActivityLogService
            adminActivityLogService;
    private final ApplicationEventPublisher
            eventPublisher;

    public AdminPolicyFormDTO createEmptyForm() {
        AdminPolicyFormDTO dto =
                new AdminPolicyFormDTO();

        dto.setCategory(
                PolicyCategory.LIVING_SUPPORT
        );
        dto.setRegion("서울특별시");
        dto.setApplyStatus(ApplyStatus.OPEN);
        dto.setApplyMethod("온라인");

        return dto;
    }

    public AdminPolicyFormDTO getPolicyForm(
            Long policyId
    ) {
        Policy policy = policyRepository
                .findWithSourceAndDetailByPolicyId(policyId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "정책을 찾을 수 없습니다."
                        )
                );

        return AdminPolicyFormDTO.from(policy);
    }

    @Transactional
    public Long savePolicy(
            AdminPolicyFormDTO form,
            String saveAction
    ) {
        validateForm(form);

        boolean isNewPolicy =
                form.getPolicyId() == null;

        Policy policy =
                findOrCreatePolicy(
                        form.getPolicyId()
                );

        PolicySource source =
                findOrCreateManualSource(
                        form.getAgency(),
                        form.getOfficialUrl()
                );

        policy.setSource(source);

        policy.setTitle(
                form.getTitle().trim()
        );

        policy.setCategory(
                form.getCategory()
        );

        policy.setTarget(
                trimToNull(form.getTarget())
        );

        policy.setRegion(
                trimToNull(form.getRegion())
        );

        policy.setDistrict(
                trimToNull(form.getDistrict())
        );

        policy.setStartDate(
                form.getStartDate()
        );

        policy.setEndDate(
                form.getEndDate()
        );

        policy.setApplyStatus(
                form.getApplyStatus() != null
                        ? form.getApplyStatus()
                        : ApplyStatus.OPEN
        );

        policy.setApplyMethod(
                trimToNull(form.getApplyMethod())
        );

        policy.setOfficialUrl(
                trimToNull(form.getOfficialUrl())
        );

        policy.setContact(
                trimToNull(form.getContact())
        );

        if (policy.getExternalId() == null
                || policy.getExternalId().isBlank()) {

            policy.setExternalId(
                    "MANUAL-" + UUID.randomUUID()
            );
        }

        PolicyStatus policyStatus =
                resolvePolicyStatus(
                        saveAction
                );

        policy.setStatus(
                policyStatus
        );

        PolicyDetail detail =
                policy.getDetail();

        if (detail == null) {
            detail = new PolicyDetail();
            policy.attachDetail(detail);
        }

        detail.setBenefit(
                trimToNull(form.getBenefit())
        );

        detail.setSelectionCriteria(
                trimToNull(
                        form.getSelectionCriteria()
                )
        );

        detail.setRequiredDocumentsText(
                trimToNull(
                        form.getRequiredDocumentsText()
                )
        );

        detail.setContentText(
                trimToNull(form.getContentText())
        );

        detail.setContentHtml(null);

        Policy savedPolicy =
                policyRepository.save(policy);

        AdminActivityType activityType =
                resolvePolicyActivityType(
                        isNewPolicy,
                        saveAction
                );

        adminActivityLogService.recordCurrentAdmin(
                activityType,
                savedPolicy.getPolicyId(),
                savedPolicy.getTitle(),
                createPolicyLogDescription(
                        isNewPolicy,
                        saveAction
                )
        );

        if (isNewPolicy) {
            eventPublisher.publishEvent(
                    new AiSummaryAutomationService.PolicyCreated(
                            savedPolicy.getPolicyId()
                    )
            );
        }

        return savedPolicy.getPolicyId();
    }

    private Policy findOrCreatePolicy(
            Long policyId
    ) {
        if (policyId == null) {
            return new Policy();
        }

        return policyRepository
                .findWithSourceAndDetailByPolicyId(policyId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "수정할 정책을 찾을 수 없습니다."
                        )
                );
    }

    private PolicySource findOrCreateManualSource(
            String agency,
            String officialUrl
    ) {
        String sourceName = agency.trim();

        return policySourceRepository
                .findFirstBySourceName(sourceName)
                .orElseGet(() -> {
                    PolicySource source =
                            new PolicySource();

                    source.setSourceName(sourceName);
                    source.setSourceType(
                            SourceType.MANUAL
                    );
                    source.setBaseUrl(
                            trimToNull(officialUrl)
                    );
                    source.setIsActive(true);

                    return policySourceRepository
                            .save(source);
                });
    }

    private PolicyStatus resolvePolicyStatus(
            String saveAction
    ) {
        if (saveAction == null) {
            return PolicyStatus.PENDING_REVIEW;
        }

        return switch (saveAction) {
            case "DRAFT" ->
                    PolicyStatus.HIDDEN;

            case "APPROVE" ->
                    PolicyStatus.APPROVED;

            case "REVIEW" ->
                    PolicyStatus.PENDING_REVIEW;

            default ->
                    PolicyStatus.PENDING_REVIEW;
        };
    }

    private void validateForm(
            AdminPolicyFormDTO form
    ) {
        if (form.getTitle() == null
                || form.getTitle().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "정책명을 입력해 주세요."
            );
        }

        if (form.getAgency() == null
                || form.getAgency().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "제공기관을 입력해 주세요."
            );
        }

        if (form.getCategory() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "정책 분야를 선택해 주세요."
            );
        }

        if (form.getBenefit() == null
                || form.getBenefit().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원 내용을 입력해 주세요."
            );
        }

        if (form.getStartDate() != null
                && form.getEndDate() != null
                && form.getEndDate()
                .isBefore(form.getStartDate())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "신청 마감일은 시작일보다 빠를 수 없습니다."
            );
        }
    }

    private String trimToNull(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    public AdminPolicyFormDTO createFormFromReport(
            Long reportId
    ) {
        MissingPolicyReport report =
                missingPolicyReportRepository
                        .findById(reportId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "사용자 신고를 찾을 수 없습니다."
                                )
                        );

        AdminPolicyFormDTO dto =
                createEmptyForm();

        dto.setTitle(
                trimToNull(report.getTitle())
        );

        dto.setAgency(
                "사용자 제보"
        );

        dto.setOfficialUrl(
                trimToNull(report.getSourceUrl())
        );

        dto.setBenefit(
                trimToNull(report.getContent())
        );

        dto.setContentText(
                trimToNull(report.getContent())
        );

        dto.setAdminMemo(
                createReportMemo(report)
        );

        return dto;
    }

    private String createReportMemo(
            MissingPolicyReport report
    ) {
        StringBuilder memo =
                new StringBuilder();

        memo.append("누락 정책 사용자 신고 #");
        memo.append(report.getReportId());

        if (report.getContent() != null
                && !report.getContent().isBlank()) {

            memo.append("\n");
            memo.append(report.getContent());
        }

        if (report.getAdminMemo() != null
                && !report.getAdminMemo().isBlank()) {

            memo.append("\n\n기존 관리자 메모\n");
            memo.append(report.getAdminMemo());
        }

        return memo.toString();
    }

    private AdminActivityType resolvePolicyActivityType(
            boolean isNewPolicy,
            String saveAction
    ) {
        if (isNewPolicy) {
            return AdminActivityType.POLICY_CREATE;
        }

        if ("APPROVE".equals(saveAction)) {
            return AdminActivityType.POLICY_APPROVE;
        }

        return AdminActivityType.POLICY_UPDATE;
    }

    private String createPolicyLogDescription(
            boolean isNewPolicy,
            String saveAction
    ) {
        if (isNewPolicy) {
            return switch (saveAction) {
                case "DRAFT" ->
                        "신규 정책을 임시 저장했습니다.";

                case "APPROVE" ->
                        "신규 정책을 등록하고 승인했습니다.";

                default ->
                        "신규 정책을 등록하고 검수 대기로 저장했습니다.";
            };
        }

        return switch (saveAction) {
            case "DRAFT" ->
                    "정책 정보를 수정하고 비공개 상태로 저장했습니다.";

            case "APPROVE" ->
                    "정책 정보를 수정하고 승인했습니다.";

            default ->
                    "정책 정보를 수정하고 검수 대기로 저장했습니다.";
        };
    }
}
