package com.seoulcareconnect.dto.report;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
// 만약 Spring Boot 2.x(javax) 기반이라면 위 import 두 줄을 javax.validation.constraints.*로 변경하세요.
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MissingPolicyReportRequestDto {


    private Long userId;


    private Long policyId;

    @NotBlank(message = "신고 유형을 선택해주세요.")
    private String reportType;

    @NotBlank(message = "신고 제목을 입력해주세요.")
    private String title;


    private String policyName;


    private String region;

    private String sourceUrl;

    @NotBlank(message = "상세 설명을 입력해주세요.")
    private String content;

    @AssertTrue(message = "신고 접수 동의가 필요합니다.")
    private boolean agree;
}
