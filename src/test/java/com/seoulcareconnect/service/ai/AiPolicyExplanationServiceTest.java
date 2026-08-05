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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiPolicyExplanationServiceTest {

    @Mock
    private AiPolicyExplanationRepository explanationRepository;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private AiModelGateway modelGateway;

    private AiPolicyExplanationService service;

    @BeforeEach
    void setUp() {
        service = new AiPolicyExplanationService(
                explanationRepository,
                policyRepository,
                modelGateway,
                new ObjectMapper(),
                new AiProperties()
        );
    }

    @Test
    void reportsSavingStageBeforePersistingGeneratedSummary() throws Exception {
        Policy policy = new Policy();
        ReflectionTestUtils.setField(policy, "policyId", 17L);
        policy.setTitle("중장년 일자리 지원");
        ObjectMapper objectMapper = new ObjectMapper();
        AtomicBoolean savingReported = new AtomicBoolean(false);

        when(explanationRepository
                .findFirstByPolicyPolicyIdAndReviewStatusOrderByCreatedAtDesc(
                        17L,
                        ReviewStatus.APPROVED
                ))
                .thenReturn(Optional.empty());
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
                "gpt-5-mini",
                objectMapper.readTree("""
                        {
                          "easySummary": "핵심 요약",
                          "eligibilitySummary": "지원 대상",
                          "benefitSummary": "지원 내용",
                          "applicationSummary": "신청 방법",
                          "cautionSummary": "공식 공고를 확인하세요."
                        }
                        """)
        ));
        when(explanationRepository.save(any(AiPolicyExplanation.class)))
                .thenAnswer(invocation -> {
                    assertThat(savingReported).isTrue();
                    return invocation.getArgument(0);
                });

        var result = service.generateIfMissing(
                17L,
                () -> savingReported.set(true)
        );

        assertThat(result.easySummary()).isEqualTo("핵심 요약");
        assertThat(savingReported).isTrue();
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
