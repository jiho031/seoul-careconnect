package com.seoulcareconnect.dto.admin;

import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.PolicyDetail;
import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class AdminPolicyFormDTO {

    private Long policyId;
    private Long reportId;

    // 기본 정보
    private String title;
    private String agency;
    private PolicyCategory category;
    private String target;
    private String region;
    private String district;

    // 신청 정보
    private LocalDate startDate;
    private LocalDate endDate;
    private ApplyStatus applyStatus;
    private String applyMethod;
    private String officialUrl;
    private String contact;

    // 상세 정보
    private String benefit;
    private String selectionCriteria;
    private String requiredDocumentsText;
    private String contentText;

    // 현재 DB 저장 대상이 아닌 화면 입력값
    private String adminMemo;
    private String easySummary;
    private String checkPoint;

    public static AdminPolicyFormDTO from(
            Policy policy
    ) {
        AdminPolicyFormDTO dto =
                new AdminPolicyFormDTO();

        dto.setPolicyId(policy.getPolicyId());
        dto.setTitle(policy.getTitle());
        dto.setCategory(policy.getCategory());
        dto.setTarget(policy.getTarget());
        dto.setRegion(policy.getRegion());
        dto.setDistrict(policy.getDistrict());
        dto.setStartDate(policy.getStartDate());
        dto.setEndDate(policy.getEndDate());
        dto.setApplyStatus(policy.getApplyStatus());
        dto.setApplyMethod(policy.getApplyMethod());
        dto.setOfficialUrl(policy.getOfficialUrl());
        dto.setContact(policy.getContact());

        if (policy.getSource() != null) {
            dto.setAgency(
                    policy.getSource().getSourceName()
            );
        }

        PolicyDetail detail = policy.getDetail();

        if (detail != null) {
            dto.setBenefit(detail.getBenefit());
            dto.setSelectionCriteria(
                    detail.getSelectionCriteria()
            );
            dto.setRequiredDocumentsText(
                    detail.getRequiredDocumentsText()
            );
            dto.setContentText(
                    detail.getContentText()
            );
        }

        return dto;
    }
}