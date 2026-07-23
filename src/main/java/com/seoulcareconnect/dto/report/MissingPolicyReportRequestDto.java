package com.seoulcareconnect.dto.report;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
// 만약 Spring Boot 2.x(javax) 기반이라면 위 import 두 줄을 javax.validation.constraints.*로 변경하세요.
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MissingPolicyReportRequestDto {

    // 로그인 사용자의 userId. 프론트(form.html)에서 hidden input으로 채워서 넘어옵니다.
    // (list.html의 data-user-id=${user.userId} 패턴과 동일하게 처리)
    private Long userId;

    // 특정 정책 상세페이지에서 "정보 오류"를 신고할 때만 채워짐. 누락 신고는 null.
    private Long policyId;

    @NotBlank(message = "신고 유형을 선택해주세요.")
    private String reportType;

    @NotBlank(message = "신고 제목을 입력해주세요.")
    private String title;

    // 화면 입력용 (엔티티에 별도 컬럼 없음 → content에 합쳐서 저장)
    private String policyName;

    // 화면 입력용 (엔티티에 별도 컬럼 없음 → content에 합쳐서 저장)
    private String region;

    private String sourceUrl;

    @NotBlank(message = "상세 설명을 입력해주세요.")
    private String content;

    @AssertTrue(message = "신고 접수 동의가 필요합니다.")
    private boolean agree;
}
