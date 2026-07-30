package com.seoulcareconnect.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulcareconnect.config.ai.AiProperties;
import com.seoulcareconnect.entity.ai.AiPolicyExplanation;
import com.seoulcareconnect.entity.ai.AiPolicyExplanation.ReviewStatus;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.repository.ai.AiPolicyExplanationRepository;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiPolicyExplanationServiceTest {

    @Mock
    private AiPolicyExplanationRepository explanationRepository;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private AiModelGateway modelGateway;

    @Mock
    private AiSummarySettingService settingService;

    private AiPolicyExplanationService service;

    @BeforeEach
    void setUp() {
        service = new AiPolicyExplanationService(
                explanationRepository,
                policyRepository,
                modelGateway,
                new ObjectMapper(),
                new AiProperties(),
                settingService
        );
    }

    @Test
    void blocksNewUserRequestedSummaryWhenAutomaticGenerationIsDisabled() {
        when(explanationRepository
                .findFirstByPolicyPolicyIdAndReviewStatusOrderByCreatedAtDesc(
                        17L,
                        ReviewStatus.APPROVED
                ))
                .thenReturn(Optional.empty());
        when(settingService.isAutomaticSummaryEnabled()).thenReturn(false);

        assertThatThrownBy(() -> service.getOrCreate(17L))
                .isInstanceOf(AiPolicyExplanationService.GenerationDisabledException.class)
                .hasMessageContaining("자동 AI 요약");

        verifyNoInteractions(policyRepository, modelGateway);
    }

    @Test
    void returnsStoredSummaryWhenAutomaticGenerationIsDisabled() {
        Policy policy = new Policy();
        ReflectionTestUtils.setField(policy, "policyId", 17L);
        policy.setTitle("중장년 일자리 지원");
        AiPolicyExplanation explanation = AiPolicyExplanation.generated(
                policy,
                new AiPolicyExplanation.Content(
                        "핵심 요약",
                        "지원 대상",
                        "지원 내용",
                        "신청 방법",
                        "주의사항"
                ),
                "gpt-5-mini",
                "policy-easy-v1"
        );
        ReflectionTestUtils.setField(explanation, "explanationId", 31L);
        when(explanationRepository
                .findFirstByPolicyPolicyIdAndReviewStatusOrderByCreatedAtDesc(
                        17L,
                        ReviewStatus.APPROVED
                ))
                .thenReturn(Optional.of(explanation));

        assertThat(service.getOrCreate(17L).easySummary()).isEqualTo("핵심 요약");

        verifyNoInteractions(settingService, policyRepository, modelGateway);
    }
}
