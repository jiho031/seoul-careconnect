package com.seoulcareconnect.service.impl.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulcareconnect.config.ai.AiProperties;
import com.seoulcareconnect.dto.ai.AiAssistantGroundingDocument;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.integration.client.ai.OpenAiEmbeddingClient;
import com.seoulcareconnect.repository.ai.AiPolicyEmbeddingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiPolicyEmbeddingServiceTest {

    @Mock
    private AiPolicyEmbeddingRepository embeddingRepository;

    @Mock
    private OpenAiEmbeddingClient embeddingClient;

    @Test
    void ranksCandidatesByCosineSimilarityAndCachesEmbeddings() {
        AiProperties properties = new AiProperties();
        properties.setEmbeddingDimensions(3);
        properties.setAssistantTopK(1);
        AiPolicyEmbeddingService service = new AiPolicyEmbeddingService(
                embeddingRepository,
                embeddingClient,
                new ObjectMapper(),
                properties
        );

        AiAssistantCandidate first = candidate(1L, "취업 지원");
        AiAssistantCandidate second = candidate(2L, "주거 지원");

        when(embeddingRepository.findAllByPolicyPolicyIdIn(anyList()))
                .thenReturn(List.of());
        when(embeddingClient.embed(anyList()))
                .thenReturn(List.of(
                        new double[]{1, 0, 0},
                        new double[]{1, 0, 0},
                        new double[]{0, 1, 0}
                ));

        List<AiAssistantCandidate> ranked = service.rank(
                "취업 지원",
                List.of(first, second)
        );

        assertThat(ranked).containsExactly(first);
        verify(embeddingRepository).saveAll(anyList());
    }

    private AiAssistantCandidate candidate(Long policyId, String text) {
        Policy policy = new Policy();
        policy.setPolicyId(policyId);
        policy.setTitle(text);
        AiAssistantGroundingDocument document = new AiAssistantGroundingDocument(
                0,
                policyId,
                text,
                "서울시",
                "일자리",
                "중장년",
                "서울시 전체",
                "상시 신청",
                "2026.07.25",
                "온라인",
                "공식 공고 확인",
                text,
                null,
                null,
                text,
                null,
                null,
                null,
                null,
                null,
                null,
                text
        );
        return new AiAssistantCandidate(policy, document, 0);
    }
}
