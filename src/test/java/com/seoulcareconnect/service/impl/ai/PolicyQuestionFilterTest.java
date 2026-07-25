package com.seoulcareconnect.service.impl.ai;

import com.seoulcareconnect.dto.ai.AiAssistantUserContext;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyQuestionFilterTest {

    private final PolicyQuestionFilter filter = new PolicyQuestionFilter();

    @Test
    void matchesDistrictAgeAndCategory() {
        Policy policy = policy("강서구", "중장년 구직자", PolicyCategory.JOB);

        assertThat(filter.matches(
                "강서구에 사는 50대가 받을 수 있는 취업 지원이 있나요?",
                policy
        )).isTrue();
    }

    @Test
    void rejectsDifferentDistrict() {
        Policy policy = policy("송파구", "중장년 구직자", PolicyCategory.JOB);

        assertThat(filter.matches(
                "강서구에 사는 50대가 받을 수 있는 취업 지원이 있나요?",
                policy
        )).isFalse();
    }

    @Test
    void enrichesQuestionOnlyWhenProfileConditionIsMissing() {
        AiAssistantUserContext context = new AiAssistantUserContext("강서구", "50대");

        assertThat(filter.enrichWithProfile("취업 정책을 찾아주세요.", context))
                .contains("사용자 지역: 강서구")
                .contains("사용자 연령대: 50대");
        assertThat(filter.enrichWithProfile("송파구 60대 취업 정책을 찾아주세요.", context))
                .doesNotContain("사용자 지역: 강서구")
                .doesNotContain("사용자 연령대: 50대");
    }

    private Policy policy(
            String district,
            String target,
            PolicyCategory category
    ) {
        Policy policy = new Policy();
        policy.setTitle("중장년 취업 지원");
        policy.setDistrict(district);
        policy.setTarget(target);
        policy.setCategory(category);
        return policy;
    }
}
