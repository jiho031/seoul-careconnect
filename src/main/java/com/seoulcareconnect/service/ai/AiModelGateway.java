package com.seoulcareconnect.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulcareconnect.config.ai.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiModelGateway {

    private static final String UNAVAILABLE_MESSAGE =
            "AI 기능을 일시적으로 이용할 수 없습니다. OpenAI와 Ollama 연결 상태를 확인해 주세요.";
    private static final String OPENAI_SUMMARY_CONFIGURATION_MESSAGE =
            "AI 정책 요약을 생성하려면 서버 환경변수 OPENAI_API_KEY를 설정해 주세요.";
    private static final String OPENAI_SUMMARY_UNAVAILABLE_MESSAGE =
            "OpenAI 정책 요약을 생성하지 못했습니다. OPENAI_API_KEY와 OpenAI 연결 상태를 확인해 주세요.";

    private final ObjectMapper objectMapper;
    private final AiProperties properties;

    public GeneratedJson generateJson(
            String schemaName,
            String systemPrompt,
            List<Message> messages,
            Map<String, Object> schema,
            int maxOutputTokens,
            List<String> requiredTextFields
    ) {
        return withFallback(
                "답변 생성",
                () -> validateRequiredTextFields(
                        openAiGenerate(schemaName, systemPrompt, messages, schema, maxOutputTokens),
                        requiredTextFields
                ),
                () -> validateRequiredTextFields(
                        ollamaGenerate(systemPrompt, messages, schema, maxOutputTokens),
                        requiredTextFields
                )
        );
    }

    public GeneratedJson generateJsonOpenAiOnly(
            String schemaName,
            String systemPrompt,
            List<Message> messages,
            Map<String, Object> schema,
            int maxOutputTokens,
            List<String> requiredTextFields
    ) {
        ensureEnabled();
        if (!hasOpenAiConfiguration()) {
            throw new UnavailableException(OPENAI_SUMMARY_CONFIGURATION_MESSAGE, null);
        }

        try {
            return validateRequiredTextFields(
                    openAiGenerate(schemaName, systemPrompt, messages, schema, maxOutputTokens),
                    requiredTextFields
            );
        } catch (UnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("OpenAI 정책 요약 생성 실패: {}", exception.getClass().getSimpleName());
            throw new UnavailableException(OPENAI_SUMMARY_UNAVAILABLE_MESSAGE, exception);
        }
    }

    public EmbeddingBatch embed(List<String> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return new EmbeddingBatch(preferredEmbeddingProfile(), List.of());
        }

        return withFallback(
                "임베딩 생성",
                () -> openAiEmbed(inputs),
                () -> ollamaEmbed(inputs)
        );
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public boolean isOpenAiConfigured() {
        return properties.isEnabled() && hasOpenAiConfiguration();
    }

    public String openAiModelName() {
        return properties.getModel();
    }

    public String preferredGenerationModelName() {
        return hasOpenAiConfiguration()
                ? properties.getModel()
                : properties.getOllamaModel();
    }

    public EmbeddingProfile preferredEmbeddingProfile() {
        if (hasOpenAiConfiguration()) {
            return new EmbeddingProfile(
                    Provider.OPENAI,
                    properties.getEmbeddingModel(),
                    properties.getEmbeddingDimensions()
            );
        }

        return new EmbeddingProfile(
                Provider.OLLAMA,
                properties.getOllamaEmbeddingModel(),
                properties.getOllamaEmbeddingDimensions()
        );
    }

    private GeneratedJson openAiGenerate(
            String schemaName,
            String systemPrompt,
            List<Message> messages,
            Map<String, Object> schema,
            int maxOutputTokens
    ) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.getModel());
        request.put("store", false);
        request.put("input", conversation(systemPrompt, messages));
        request.put("text", Map.of("format", Map.of(
                "type", "json_schema",
                "name", schemaName,
                "strict", true,
                "schema", schema
        )));
        request.put("max_output_tokens", maxOutputTokens);

        JsonNode response = openAiClient().post()
                .uri("/responses")
                .header("Authorization", "Bearer " + properties.resolvedApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        return new GeneratedJson(
                Provider.OPENAI,
                properties.getModel(),
                parseJson(extractOpenAiText(response))
        );
    }

    private GeneratedJson ollamaGenerate(
            String systemPrompt,
            List<Message> messages,
            Map<String, Object> schema,
            int maxOutputTokens
    ) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.getOllamaModel());
        request.put("messages", conversation(systemPrompt, messages));
        request.put("format", schema);
        request.put("stream", false);
        request.put("options", Map.of("num_predict", maxOutputTokens));

        JsonNode response = ollamaClient().post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        return new GeneratedJson(
                Provider.OLLAMA,
                properties.getOllamaModel(),
                parseJson(extractOllamaText(response))
        );
    }

    private EmbeddingBatch openAiEmbed(List<String> inputs) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.getEmbeddingModel());
        request.put("input", inputs);
        request.put("encoding_format", "float");
        if (properties.getEmbeddingDimensions() > 0) {
            request.put("dimensions", properties.getEmbeddingDimensions());
        }

        JsonNode response = openAiClient().post()
                .uri("/embeddings")
                .header("Authorization", "Bearer " + properties.resolvedApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.path("data").isArray()) {
            throw new IllegalStateException("OpenAI 임베딩 응답 형식이 올바르지 않습니다.");
        }

        List<IndexedVector> indexed = new ArrayList<>();
        for (JsonNode item : response.path("data")) {
            int index = item.path("index").asInt(-1);
            if (index < 0) {
                throw new IllegalStateException("OpenAI 임베딩 순서를 확인할 수 없습니다.");
            }
            indexed.add(new IndexedVector(index, readVector(item.path("embedding"))));
        }
        indexed.sort(Comparator.comparingInt(IndexedVector::index));

        List<double[]> vectors = indexed.stream().map(IndexedVector::vector).toList();
        validateVectors(inputs, vectors);
        return new EmbeddingBatch(
                new EmbeddingProfile(Provider.OPENAI, properties.getEmbeddingModel(), vectors.get(0).length),
                vectors
        );
    }

    private EmbeddingBatch ollamaEmbed(List<String> inputs) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.getOllamaEmbeddingModel());
        request.put("input", inputs);
        if (properties.getOllamaEmbeddingDimensions() > 0) {
            request.put("dimensions", properties.getOllamaEmbeddingDimensions());
        }

        JsonNode response = ollamaClient().post()
                .uri("/api/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.path("embeddings").isArray()) {
            throw new IllegalStateException("Ollama 임베딩 응답 형식이 올바르지 않습니다.");
        }

        List<double[]> vectors = new ArrayList<>();
        for (JsonNode item : response.path("embeddings")) {
            vectors.add(readVector(item));
        }
        validateVectors(inputs, vectors);
        return new EmbeddingBatch(
                new EmbeddingProfile(Provider.OLLAMA, properties.getOllamaEmbeddingModel(), vectors.get(0).length),
                vectors
        );
    }

    private <T> T withFallback(
            String operation,
            Supplier<T> openAiCall,
            Supplier<T> ollamaCall
    ) {
        ensureEnabled();

        RuntimeException openAiFailure = null;
        if (hasOpenAiConfiguration()) {
            try {
                return openAiCall.get();
            } catch (RuntimeException exception) {
                openAiFailure = exception;
                log.warn("OpenAI {} 실패로 Ollama 대체를 시도합니다: {}", operation,
                        exception.getClass().getSimpleName());
            }
        }

        if (hasOllamaConfiguration()) {
            try {
                return ollamaCall.get();
            } catch (RuntimeException exception) {
                throw new UnavailableException(UNAVAILABLE_MESSAGE, exception);
            }
        }

        throw new UnavailableException(UNAVAILABLE_MESSAGE, openAiFailure);
    }

    private List<Map<String, String>> conversation(
            String systemPrompt,
            List<Message> messages
    ) {
        List<Map<String, String>> conversation = new ArrayList<>();
        conversation.add(Map.of("role", "system", "content", systemPrompt));
        messages.forEach(message -> conversation.add(Map.of(
                "role", message.role(),
                "content", message.content()
        )));
        return conversation;
    }

    private JsonNode parseJson(String value) {
        String json = value == null ? "" : value.trim();
        if (json.startsWith("```")) {
            int start = json.indexOf('\n');
            int end = json.lastIndexOf("```");
            if (start >= 0 && end > start) {
                json = json.substring(start + 1, end).trim();
            }
        }

        try {
            JsonNode parsed = objectMapper.readTree(json);
            if (parsed == null || !parsed.isObject()) {
                throw new IllegalStateException("AI가 JSON 객체 형식으로 답변하지 않았습니다.");
            }
            return parsed;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI JSON 응답을 처리하지 못했습니다.", exception);
        }
    }

    private GeneratedJson validateRequiredTextFields(
            GeneratedJson generated,
            List<String> requiredTextFields
    ) {
        boolean missingRequiredValue = requiredTextFields.stream()
                .map(generated.payload()::path)
                .map(JsonNode::asText)
                .anyMatch(value -> !StringUtils.hasText(value));
        if (missingRequiredValue) {
            throw new IllegalStateException("AI가 필수 응답 항목을 생성하지 못했습니다.");
        }
        return generated;
    }

    private String extractOpenAiText(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("OpenAI가 빈 응답을 반환했습니다.");
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
        throw new IllegalStateException("OpenAI 응답에서 답변을 찾지 못했습니다.");
    }

    private String extractOllamaText(JsonNode response) {
        String content = response == null ? null : response.path("message").path("content").asText(null);
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("Ollama 응답에서 답변을 찾지 못했습니다.");
        }
        return content;
    }

    private double[] readVector(JsonNode node) {
        if (!node.isArray() || node.isEmpty()) {
            throw new IllegalStateException("AI 임베딩 벡터가 비어 있습니다.");
        }

        double[] vector = new double[node.size()];
        for (int index = 0; index < node.size(); index++) {
            vector[index] = node.get(index).asDouble();
        }
        return vector;
    }

    private void validateVectors(List<String> inputs, List<double[]> vectors) {
        if (vectors.size() != inputs.size()) {
            throw new IllegalStateException("AI 임베딩 결과 개수가 요청과 일치하지 않습니다.");
        }
        int dimensions = vectors.get(0).length;
        if (vectors.stream().anyMatch(vector -> vector.length != dimensions)) {
            throw new IllegalStateException("AI 임베딩 차원이 서로 일치하지 않습니다.");
        }
    }

    private RestClient openAiClient() {
        return RestClient.builder().baseUrl(properties.getBaseUrl().trim()).build();
    }

    private RestClient ollamaClient() {
        return RestClient.builder().baseUrl(properties.getOllamaBaseUrl().trim()).build();
    }

    private void ensureEnabled() {
        if (!isEnabled()) {
            throw new UnavailableException("AI 기능이 비활성화되어 있습니다.", null);
        }
    }

    private boolean hasOpenAiConfiguration() {
        return StringUtils.hasText(properties.resolvedApiKey())
                && StringUtils.hasText(properties.getBaseUrl())
                && StringUtils.hasText(properties.getModel())
                && StringUtils.hasText(properties.getEmbeddingModel());
    }

    private boolean hasOllamaConfiguration() {
        return properties.isOllamaEnabled()
                && StringUtils.hasText(properties.getOllamaBaseUrl())
                && StringUtils.hasText(properties.getOllamaModel())
                && StringUtils.hasText(properties.getOllamaEmbeddingModel());
    }

    public enum Provider {
        OPENAI,
        OLLAMA
    }

    public record Message(String role, String content) {
    }

    public record GeneratedJson(Provider provider, String modelName, JsonNode payload) {
    }

    public record EmbeddingProfile(Provider provider, String modelName, int dimensions) {
        public boolean matches(EmbeddingProfile other) {
            return provider == other.provider
                    && modelName.equals(other.modelName)
                    && (dimensions <= 0 || dimensions == other.dimensions);
        }
    }

    public record EmbeddingBatch(EmbeddingProfile profile, List<double[]> vectors) {
    }

    private record IndexedVector(int index, double[] vector) {
    }

    public static class UnavailableException extends RuntimeException {
        public UnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
