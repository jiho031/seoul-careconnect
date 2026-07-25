package com.seoulcareconnect.integration.client.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.seoulcareconnect.config.ai.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiEmbeddingClient {

    private final @Qualifier("openAiRestClient") RestClient restClient;
    private final AiProperties properties;

    public List<double[]> embed(List<String> inputs) {
        validateConfiguration();

        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }

        Map<String, Object> request = Map.of(
                "model", properties.getEmbeddingModel(),
                "input", inputs,
                "dimensions", properties.getEmbeddingDimensions(),
                "encoding_format", "float"
        );

        JsonNode response = restClient.post()
                .uri("/embeddings")
                .header("Authorization", "Bearer " + properties.getApiKey().trim())
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.path("data").isArray()) {
            throw new IllegalStateException("AI 임베딩 서비스가 올바른 응답을 반환하지 않았습니다.");
        }

        List<IndexedVector> indexedVectors = new ArrayList<>();
        for (JsonNode item : response.path("data")) {
            int index = item.path("index").asInt(-1);
            JsonNode embedding = item.path("embedding");
            if (index < 0 || !embedding.isArray()) {
                throw new IllegalStateException("AI 임베딩 응답 형식이 올바르지 않습니다.");
            }

            double[] vector = new double[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = embedding.get(i).asDouble();
            }

            if (vector.length != properties.getEmbeddingDimensions()) {
                throw new IllegalStateException("AI 임베딩 차원이 설정과 일치하지 않습니다.");
            }
            indexedVectors.add(new IndexedVector(index, vector));
        }

        indexedVectors.sort(Comparator.comparingInt(IndexedVector::index));
        if (indexedVectors.size() != inputs.size()) {
            throw new IllegalStateException("AI 임베딩 결과 개수가 요청과 일치하지 않습니다.");
        }

        return indexedVectors.stream()
                .map(IndexedVector::vector)
                .toList();
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("AI 기능이 비활성화되어 있습니다.");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("AI API 키가 설정되지 않았습니다.");
        }
        if (!StringUtils.hasText(properties.getEmbeddingModel())) {
            throw new IllegalStateException("AI 임베딩 모델이 설정되지 않았습니다.");
        }
        if (properties.getEmbeddingDimensions() < 1) {
            throw new IllegalStateException("AI 임베딩 차원 설정이 올바르지 않습니다.");
        }
    }

    private record IndexedVector(int index, double[] vector) {
    }
}
