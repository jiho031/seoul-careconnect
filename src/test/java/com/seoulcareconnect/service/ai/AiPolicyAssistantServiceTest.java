package com.seoulcareconnect.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulcareconnect.config.ai.AiProperties;
import com.seoulcareconnect.dto.ai.AiAssistantRequest;
import com.seoulcareconnect.dto.ai.AiAssistantResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
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

    private AiPolicyAssistantService service;

    @BeforeEach
    void setUp() {
        service = new AiPolicyAssistantService(
                policyRepository,
                explanationRepository,
                embeddingRepository,
                userRepository,
                modelGateway,
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

}
