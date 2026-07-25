package com.seoulcareconnect.service.impl.ai;

import com.seoulcareconnect.config.ai.AiProperties;
import com.seoulcareconnect.dto.ai.AiAssistantRequest;
import com.seoulcareconnect.dto.ai.AiAssistantResponse;
import com.seoulcareconnect.dto.ai.AiAssistantUserContext;
import com.seoulcareconnect.integration.client.ai.OpenAiAssistantClient;
import com.seoulcareconnect.repository.ai.AiPolicyExplanationRepository;
import com.seoulcareconnect.repository.policy.PolicyRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiPolicyAssistantServiceImplTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private AiPolicyExplanationRepository explanationRepository;

    @Mock
    private PolicyQuestionFilter questionFilter;

    @Mock
    private AiPolicyEmbeddingService embeddingService;

    @Mock
    private OpenAiAssistantClient assistantClient;

    private AiPolicyAssistantServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiPolicyAssistantServiceImpl(
                policyRepository,
                explanationRepository,
                questionFilter,
                embeddingService,
                assistantClient,
                new AiProperties()
        );
        ReflectionTestUtils.setField(service, "zoneId", "Asia/Seoul");
    }

    @Test
    void returnsGroundingFailureWithoutCallingAi() {
        when(questionFilter.enrichWithProfile(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(policyRepository.findAssistantCandidates(anyList(), any(), any()))
                .thenReturn(List.of());

        AiAssistantResponse response = service.ask(
                new AiAssistantRequest("정책을 찾아주세요.", null, List.of()),
                AiAssistantUserContext.empty()
        );

        assertThat(response.grounded()).isFalse();
        assertThat(response.sources()).isEmpty();
        verify(assistantClient, never()).answer(any(), anyList(), anyList());
        verify(embeddingService, never()).rank(any(), anyList());
    }
}
