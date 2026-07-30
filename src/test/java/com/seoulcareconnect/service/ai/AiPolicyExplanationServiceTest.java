package com.seoulcareconnect.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulcareconnect.config.ai.AiProperties;
import com.seoulcareconnect.entity.ai.AiPolicyExplanation;
import com.seoulcareconnect.entity.ai.AiPolicyExplanation.ReviewStatus;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.PolicyDetail;
import com.seoulcareconnect.repository.ai.AiPolicyExplanationRepository;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
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

    @Test
    void regeneratesSummaryAfterPolicyApplicationInformationChanges() throws Exception {
        Policy policy = new Policy();
        ReflectionTestUtils.setField(policy, "policyId", 17L);
        policy.setTitle("중장년 일자리 지원");
        policy.setApplyMethod("서울일자리포털에서 온라인 신청");
        PolicyDetail detail = new PolicyDetail();
        detail.setRequiredDocumentsText("참여신청서\n주민등록등본");
        policy.attachDetail(detail);

        AiPolicyExplanation previous = AiPolicyExplanation.generated(
                policy,
                new AiPolicyExplanation.Content(
                        "이전 핵심 요약",
                        "이전 지원 대상",
                        "이전 지원 내용",
                        "이전 신청 방법",
                        "이전 주의사항"
                ),
                "old-model",
                "policy-easy-v1"
        );

        ObjectMapper objectMapper = new ObjectMapper();
        when(policyRepository.findWithSourceAndDetailByPolicyId(17L))
                .thenReturn(Optional.of(policy));
        when(modelGateway.generateJsonOpenAiOnly(
                anyString(),
                anyString(),
                anyList(),
                anyMap(),
                anyInt(),
                anyList()
        )).thenReturn(new AiModelGateway.GeneratedJson(
                AiModelGateway.Provider.OPENAI,
                "new-model",
                objectMapper.readTree("""
                        {
                          "easySummary": "새 핵심 요약",
                          "eligibilitySummary": "새 지원 대상",
                          "benefitSummary": "새 지원 내용",
                          "applicationSummary": "온라인 신청 후 서류를 제출하세요.",
                          "cautionSummary": "공식 공고를 확인하세요."
                        }
                        """)
        ));
        when(explanationRepository.findByPolicyPolicyIdAndReviewStatus(
                17L,
                ReviewStatus.APPROVED
        )).thenReturn(List.of(previous));
        when(explanationRepository.save(any(AiPolicyExplanation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.regenerate(17L);

        assertThat(previous.getReviewStatus()).isEqualTo(ReviewStatus.SUPERSEDED);
        assertThat(result.easySummary()).isEqualTo("새 핵심 요약");
        assertThat(result.applicationSummary())
                .isEqualTo("온라인 신청 후 서류를 제출하세요.");
        verify(explanationRepository).save(any(AiPolicyExplanation.class));
    }
}
