package com.seoulcareconnect.service.admin;

import com.seoulcareconnect.dto.admin.AdminPolicyFormDTO;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.PolicyDetail;
import com.seoulcareconnect.entity.policy.PolicySource;
import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.entity.policy.enums.PolicyStatus;
import com.seoulcareconnect.entity.policy.enums.SourceType;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import com.seoulcareconnect.repository.policy.PolicySourceRepository;
import lombok.RequiredArgsConstructor;
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

    /**
     * 신규 등록 화면 기본값
     */
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

    /**
     * 수정 화면 데이터 조회
     */
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

    /**
     * 정책 등록 또는 수정
     */
    @Transactional
    public Long savePolicy(
            AdminPolicyFormDTO form,
            String saveAction
    ) {
        validateForm(form);

        Policy policy = findOrCreatePolicy(
                form.getPolicyId()
        );

        PolicySource source =
                findOrCreateManualSource(
                        form.getAgency(),
                        form.getOfficialUrl()
                );

        policy.setSource(source);
        policy.setTitle(form.getTitle().trim());
        policy.setCategory(form.getCategory());
        policy.setTarget(trimToNull(form.getTarget()));
        policy.setRegion(trimToNull(form.getRegion()));
        policy.setDistrict(trimToNull(form.getDistrict()));
        policy.setStartDate(form.getStartDate());
        policy.setEndDate(form.getEndDate());
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

        /*
         * 외부 API 식별자가 없는 수동 등록 정책은
         * 신규 등록 시에만 고유 식별자를 생성한다.
         */
        if (policy.getExternalId() == null
                || policy.getExternalId().isBlank()) {

            policy.setExternalId(
                    "MANUAL-" + UUID.randomUUID()
            );
        }

        policy.setStatus(
                resolvePolicyStatus(saveAction)
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
                trimToNull(form.getSelectionCriteria())
        );
        detail.setRequiredDocumentsText(
                trimToNull(
                        form.getRequiredDocumentsText()
                )
        );
        detail.setContentText(
                trimToNull(form.getContentText())
        );

        /*
         * 수동 입력은 일반 텍스트이므로
         * contentHtml은 생성하지 않는다.
         */
        detail.setContentHtml(null);

        Policy savedPolicy =
                policyRepository.save(policy);

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

    /**
     * 제공기관명을 기준으로 수동 등록용 출처 조회 또는 생성
     */
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
}