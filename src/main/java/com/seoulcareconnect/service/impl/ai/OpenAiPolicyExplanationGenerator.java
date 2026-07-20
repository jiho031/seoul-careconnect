package com.seoulcareconnect.service.impl.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulcareconnect.config.ai.AiProperties;
import com.seoulcareconnect.dto.ai.AiPolicyExplanationContent;
import com.seoulcareconnect.entity.policy.Policy;
import com.seoulcareconnect.entity.policy.PolicyDetail;
import com.seoulcareconnect.service.ai.PolicyExplanationGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiPolicyExplanationGenerator implements PolicyExplanationGenerator {

    private static final int FIELD_LIMIT = 8_000;

    private static final String SYSTEM_PROMPT = """
            당신은 서울시 정책 정보를 시민이 이해하기 쉬운 한국어로 바꾸는 행정 정보 편집자입니다.
            제공된 정책 원문만 근거로 사용하세요. 원문에 없는 자격, 금액, 날짜, 신청 링크를 추측하거나 만들지 마세요.
            나이·소득·거주지·기간·금액 등 숫자 조건은 원문과 정확히 일치시켜야 합니다.
            불명확하거나 없는 정보는 '공식 공고에서 확인이 필요합니다'라고 명시하세요.
            신청 가능 여부를 확정적으로 판정하지 말고, 쉬운 존댓말을 사용하세요.
            각 항목은 중복 없이 1~3개의 짧은 문장으로 작성하세요.
            """;

    private final @Qualifier("openAiRestClient") RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;

    @Override
    public AiPolicyExplanationContent generate(Policy policy) {
        validateConfiguration();

        Map<String, Object> request = Map.of(
                "model", properties.getModel(),
                "store", false,
                "input", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", buildPolicySource(policy))
                ),
                "text", Map.of("format", responseFormat()),
                "max_output_tokens", properties.getMaxOutputTokens()
        );

        JsonNode response = restClient.post()
                .uri("/responses")
                .header("Authorization", "Bearer " + properties.getApiKey().trim())
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        String outputText = extractOutputText(response);
        try {
            AiPolicyExplanationContent content = objectMapper.readValue(
                    outputText,
                    AiPolicyExplanationContent.class
            );
            validateContent(content);
            return content;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 응답을 정책 설명 형식으로 변환하지 못했습니다.", exception);
        }
    }

    private Map<String, Object> responseFormat() {
        Map<String, Object> propertiesSchema = Map.of(
                "easySummary", textField("정책의 목적과 핵심 지원을 쉬운 말로 요약"),
                "eligibilitySummary", textField("지원 대상과 핵심 자격 조건을 쉬운 말로 요약"),
                "benefitSummary", textField("지원 내용과 금액·횟수 조건을 쉬운 말로 요약"),
                "applicationSummary", textField("신청 방법·기간·문의처를 쉬운 말로 요약"),
                "cautionSummary", textField("누락 정보와 공식 공고 확인이 필요한 주의사항")
        );

        Map<String, Object> schema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", propertiesSchema,
                "required", List.of(
                        "easySummary",
                        "eligibilitySummary",
                        "benefitSummary",
                        "applicationSummary",
                        "cautionSummary"
                )
        );

        return Map.of(
                "type", "json_schema",
                "name", "policy_easy_explanation",
                "strict", true,
                "schema", schema
        );
    }

    private Map<String, Object> textField(String description) {
        return Map.of(
                "type", "string",
                "description", description
        );
    }

    private String buildPolicySource(Policy policy) {
        PolicyDetail detail = policy.getDetail();
        String sourceName = policy.getSource() == null ? null : policy.getSource().getSourceName();

        return """
                다음 정책 원문을 시민용 쉬운 설명으로 정리하세요.

                [정책명] %s
                [제공기관] %s
                [분야] %s
                [지원 대상] %s
                [지역] %s %s
                [신청 시작일] %s
                [신청 종료일] %s
                [신청 상태] %s
                [신청 방법] %s
                [문의처] %s
                [공식 URL] %s
                [지원 내용] %s
                [선정 기준] %s
                [필요 서류] %s
                [상세 원문] %s
                """.formatted(
                safe(policy.getTitle()),
                safe(sourceName),
                safe(policy.getCategory()),
                safe(policy.getTarget()),
                safe(policy.getRegion()),
                safe(policy.getDistrict()),
                safe(policy.getStartDate()),
                safe(policy.getEndDate()),
                safe(policy.getApplyStatus()),
                safe(policy.getApplyMethod()),
                safe(policy.getContact()),
                safe(policy.getOfficialUrl()),
                safe(detail == null ? null : detail.getBenefit()),
                safe(detail == null ? null : detail.getSelectionCriteria()),
                safe(detail == null ? null : detail.getRequiredDocumentsText()),
                safe(detail == null ? null : detail.getContentText())
        );
    }

    private String safe(Object value) {
        if (value == null) return "정보 없음";
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return "정보 없음";
        return text.length() <= FIELD_LIMIT ? text : text.substring(0, FIELD_LIMIT);
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("AI 서비스가 빈 응답을 반환했습니다.");
        }

        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText())
                        && content.path("text").isTextual()) {
                    return content.path("text").asText();
                }
            }
        }

        if (response.path("output_text").isTextual()) {
            return response.path("output_text").asText();
        }

        throw new IllegalStateException("AI 응답에서 정책 설명을 찾지 못했습니다.");
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("AI 기능이 비활성화되어 있습니다. APP_AI_ENABLED를 확인해 주세요.");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("AI API 키가 없습니다. APP_AI_API_KEY를 확인해 주세요.");
        }
    }

    private void validateContent(AiPolicyExplanationContent content) {
        if (content == null
                || !StringUtils.hasText(content.easySummary())
                || !StringUtils.hasText(content.eligibilitySummary())
                || !StringUtils.hasText(content.benefitSummary())
                || !StringUtils.hasText(content.applicationSummary())
                || !StringUtils.hasText(content.cautionSummary())) {
            throw new IllegalStateException("AI가 필수 정책 설명 항목을 모두 생성하지 못했습니다.");
        }
    }
}
