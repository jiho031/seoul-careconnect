package com.seoulcareconnect.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulcareconnect.config.ai.AiProperties;
import com.seoulcareconnect.integration.policy.ExternalPolicyItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiPolicyApplicationExtractionServiceTest {

    @Mock
    private AiModelGateway modelGateway;

    private ObjectMapper objectMapper;
    private AiPolicyApplicationExtractionService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new AiPolicyApplicationExtractionService(
                modelGateway,
                objectMapper,
                new AiProperties()
        );
    }

    @Test
    void keepsOnlyApplicationInformationGroundedInOfficialText() throws Exception {
        String officialText = """
                신청방법
                서울일자리포털에서 온라인으로 신청합니다.
                제출서류
                참여신청서 1부
                주민등록등본 1부
                """;
        JsonNode payload = objectMapper.readTree("""
                {
                  "applicationMethod": "서울일자리포털에서 온라인으로 신청",
                  "applicationEvidence": "서울일자리포털에서 온라인으로 신청합니다.",
                  "requiredDocuments": [
                    {
                      "name": "참여신청서",
                      "evidence": "참여신청서 1부"
                    },
                    {
                      "name": "주민등록등본",
                      "evidence": "주민등록등본 1부"
                    }
                  ]
                }
                """);
        when(modelGateway.generateJson(
                anyString(),
                anyString(),
                anyList(),
                anyMap(),
                anyInt(),
                anyList()
        )).thenReturn(new AiModelGateway.GeneratedJson(
                AiModelGateway.Provider.OPENAI,
                "test-model",
                payload
        ));

        AiPolicyApplicationExtractionService.Extraction result = service.extract(
                ExternalPolicyItem.builder()
                        .title("중장년 일자리 지원")
                        .officialUrl("https://example.go.kr/policy")
                        .build(),
                officialText
        );

        assertThat(result.applicationMethod())
                .isEqualTo("서울일자리포털에서 온라인으로 신청");
        assertThat(result.documentNames())
                .containsExactly("참여신청서", "주민등록등본");
        assertThat(result.provider()).isEqualTo("OPENAI");
        assertThat(result.modelName()).isEqualTo("test-model");
    }

    @Test
    void rejectsValuesWhoseEvidenceIsMissingFromOfficialText() throws Exception {
        String officialText = "신청은 홈페이지에서 온라인으로 접수합니다.";
        JsonNode payload = objectMapper.readTree("""
                {
                  "applicationMethod": "주민센터 방문 신청",
                  "applicationEvidence": "주민센터에 방문해 신청합니다.",
                  "requiredDocuments": [
                    {
                      "name": "소득증명서",
                      "evidence": "소득증명서를 제출합니다."
                    }
                  ]
                }
                """);
        when(modelGateway.generateJson(
                anyString(),
                anyString(),
                anyList(),
                anyMap(),
                anyInt(),
                anyList()
        )).thenReturn(new AiModelGateway.GeneratedJson(
                AiModelGateway.Provider.OLLAMA,
                "test-ollama",
                payload
        ));

        AiPolicyApplicationExtractionService.Extraction result = service.extract(
                ExternalPolicyItem.builder().title("정책").build(),
                officialText
        );

        assertThat(result.applicationMethod()).isNull();
        assertThat(result.requiredDocuments()).isEmpty();
    }

    @Test
    void rejectsApplicationMethodThatWasNotCopiedFromOfficialText() throws Exception {
        String officialText = "신청은 홈페이지에서 온라인으로 접수합니다.";
        JsonNode payload = objectMapper.readTree("""
                {
                  "applicationMethod": "정부24에서 온라인으로 신청합니다.",
                  "applicationEvidence": "신청은 홈페이지에서 온라인으로 접수합니다.",
                  "requiredDocuments": []
                }
                """);
        when(modelGateway.generateJson(
                anyString(),
                anyString(),
                anyList(),
                anyMap(),
                anyInt(),
                anyList()
        )).thenReturn(new AiModelGateway.GeneratedJson(
                AiModelGateway.Provider.OPENAI,
                "test-model",
                payload
        ));

        AiPolicyApplicationExtractionService.Extraction result = service.extract(
                ExternalPolicyItem.builder().title("정책").build(),
                officialText
        );

        assertThat(result.applicationMethod()).isNull();
    }

    @Test
    void reusesGroundedCacheForTheCurrentModelAndPrompt() {
        String officialText = """
                홈페이지에서 온라인 신청합니다.
                참여신청서 1부를 제출합니다.
                """;
        AiPolicyApplicationExtractionService.Extraction cached =
                new AiPolicyApplicationExtractionService.Extraction(
                        "홈페이지에서 온라인 신청",
                        "홈페이지에서 온라인 신청합니다.",
                        List.of(new AiPolicyApplicationExtractionService.DocumentEvidence(
                                "참여신청서",
                                "참여신청서 1부를 제출합니다."
                        )),
                        "OPENAI",
                        "gpt-5.6",
                        "policy-application-extract-v1"
                );

        var restored = service.readCached(service.writeCache(cached), officialText);

        assertThat(restored).isPresent();
        assertThat(restored.orElseThrow().documentNames())
                .containsExactly("참여신청서");
    }
}
