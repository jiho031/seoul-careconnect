package com.seoulcareconnect.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulcareconnect.config.ai.AiProperties;
import com.seoulcareconnect.dto.ai.AiAssistantRequest;
import com.seoulcareconnect.dto.ai.AiAssistantResponse;
import com.seoulcareconnect.dto.ai.AiPolicyExplanationDto;
import com.seoulcareconnect.entity.ai.AiPolicyExplanation;
import com.seoulcareconnect.entity.ai.AiPolicyExplanation.ReviewStatus;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.repository.ai.AiPolicyEmbeddingRepository;
import com.seoulcareconnect.repository.ai.AiPolicyExplanationRepository;
import com.seoulcareconnect.repository.policy.PolicyRepository;
import com.seoulcareconnect.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiPolicyAssistantServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private AiPolicyExplanationRepository explanationRepository;

    @Mock
    private AiPolicyEmbeddingRepository embeddingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AiModelGateway modelGateway;

    @Mock
    private AiPolicyExplanationService explanationService;

    private AiPolicyAssistantService service;

    @BeforeEach
    void setUp() {
        service = new AiPolicyAssistantService(
                policyRepository,
                explanationRepository,
                embeddingRepository,
                userRepository,
                modelGateway,
                explanationService,
                new ObjectMapper(),
                new AiProperties()
        );
        ReflectionTestUtils.setField(service, "zoneId", "Asia/Seoul");
    }

    @Test
    void returnsGroundingFailureWithoutCallingAi() {
        when(policyRepository.findAssistantCandidates(anyList(), any(), any()))
                .thenReturn(List.of());

        AiAssistantResponse response = service.ask(
                new AiAssistantRequest("정책을 찾아주세요.", null, List.of()),
                null
        );

        assertThat(response.grounded()).isFalse();
        assertThat(response.sources()).isEmpty();
        verifyNoInteractions(modelGateway);
    }

    @Test
    void embeddingProfileDoesNotMixOpenAiAndOllamaVectors() {
        AiModelGateway.EmbeddingProfile openAi = new AiModelGateway.EmbeddingProfile(
                AiModelGateway.Provider.OPENAI,
                "text-embedding-3-small",
                512
        );
        AiModelGateway.EmbeddingProfile ollama = new AiModelGateway.EmbeddingProfile(
                AiModelGateway.Provider.OLLAMA,
                "embeddinggemma",
                768
        );

        assertThat(openAi.matches(ollama)).isFalse();
    }

    @Test
    void initializesPolicyWithStoredOrGeneratedSummaryAsFirstAnswer() {
        Policy policy = new Policy();
        ReflectionTestUtils.setField(policy, "policyId", 17L);
        policy.setTitle("중장년 일자리 지원");
        AiPolicyExplanation explanation = AiPolicyExplanation.generated(
                policy,
                new AiPolicyExplanation.Content(
                        "핵심 요약",
                        "지원 대상 정리",
                        "지원 내용 정리",
                        "신청 방법 정리",
                        "주의사항"
                ),
                "gpt-5.6",
                "policy-easy-v1"
        );
        ReflectionTestUtils.setField(explanation, "explanationId", 31L);
        AiPolicyExplanationDto dto = new AiPolicyExplanationDto(
                31L,
                17L,
                policy.getTitle(),
                ReviewStatus.APPROVED,
                "생성 완료",
                "핵심 요약",
                "지원 대상 정리",
                "지원 내용 정리",
                "신청 방법 정리",
                "주의사항",
                "gpt-5.6",
                "policy-easy-v1",
                null,
                null,
                null,
                null,
                null
        );

        when(policyRepository.findAssistantPolicy(any(), anyList(), any(), any()))
                .thenReturn(Optional.of(policy));
        when(explanationService.getOrCreate(17L)).thenReturn(dto);
        when(explanationRepository
                .findFirstByPolicyPolicyIdAndReviewStatusOrderByCreatedAtDesc(
                        17L,
                        ReviewStatus.APPROVED
                ))
                .thenReturn(Optional.of(explanation));

        AiAssistantResponse response = service.initializePolicy(17L);

        assertThat(response.answer())
                .contains("핵심 요약")
                .contains("지원 대상 정리")
                .contains("이어서 궁금한 내용을 질문해 주세요.");
        assertThat(response.sources()).hasSize(1);
        verify(explanationService).getOrCreate(17L);
        verifyNoInteractions(modelGateway);
    }
}
