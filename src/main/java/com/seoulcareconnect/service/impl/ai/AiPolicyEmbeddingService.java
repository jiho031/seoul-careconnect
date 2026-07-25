package com.seoulcareconnect.service.impl.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulcareconnect.config.ai.AiProperties;
import com.seoulcareconnect.entity.ai.AiPolicyEmbedding;
import com.seoulcareconnect.integration.client.ai.OpenAiEmbeddingClient;
import com.seoulcareconnect.repository.ai.AiPolicyEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiPolicyEmbeddingService {

    private final AiPolicyEmbeddingRepository embeddingRepository;
    private final OpenAiEmbeddingClient embeddingClient;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;

    @Transactional
    public synchronized List<AiAssistantCandidate> rank(
            String question,
            List<AiAssistantCandidate> candidates
    ) {
        if (candidates.isEmpty()) return List.of();

        List<Long> policyIds = candidates.stream()
                .map(candidate -> candidate.policy().getPolicyId())
                .toList();
        Map<Long, AiPolicyEmbedding> stored = embeddingRepository
                .findAllByPolicyPolicyIdIn(policyIds)
                .stream()
                .collect(Collectors.toMap(
                        embedding -> embedding.getPolicy().getPolicyId(),
                        Function.identity()
                ));

        List<AiAssistantCandidate> stale = candidates.stream()
                .filter(candidate -> {
                    AiPolicyEmbedding embedding = stored.get(candidate.policy().getPolicyId());
                    return embedding == null || !embedding.matches(
                            properties.getEmbeddingModel(),
                            properties.getEmbeddingDimensions(),
                            hash(candidate.document().embeddingText())
                    );
                })
                .toList();

        List<String> embeddingInputs = new ArrayList<>();
        embeddingInputs.add(question);
        stale.forEach(candidate -> embeddingInputs.add(candidate.document().embeddingText()));
        List<double[]> generated = embeddingClient.embed(embeddingInputs);
        double[] questionVector = generated.get(0);

        for (int i = 0; i < stale.size(); i++) {
            AiAssistantCandidate candidate = stale.get(i);
            double[] vector = generated.get(i + 1);
            String contentHash = hash(candidate.document().embeddingText());
            String vectorJson = writeVector(vector);
            AiPolicyEmbedding embedding = stored.get(candidate.policy().getPolicyId());

            if (embedding == null) {
                embedding = AiPolicyEmbedding.create(
                        candidate.policy(),
                        properties.getEmbeddingModel(),
                        properties.getEmbeddingDimensions(),
                        contentHash,
                        vectorJson
                );
                stored.put(candidate.policy().getPolicyId(), embedding);
            } else {
                embedding.update(
                        properties.getEmbeddingModel(),
                        properties.getEmbeddingDimensions(),
                        contentHash,
                        vectorJson
                );
            }
        }
        if (!stale.isEmpty()) {
            embeddingRepository.saveAll(stale.stream()
                    .map(candidate -> stored.get(candidate.policy().getPolicyId()))
                    .toList());
        }

        Map<Long, Double> similarities = new HashMap<>();
        for (AiAssistantCandidate candidate : candidates) {
            double[] vector = readVector(stored.get(candidate.policy().getPolicyId()).getVectorJson());
            similarities.put(
                    candidate.policy().getPolicyId(),
                    cosineSimilarity(questionVector, vector)
            );
        }

        return candidates.stream()
                .sorted(Comparator
                        .comparingDouble((AiAssistantCandidate candidate) ->
                                similarities.get(candidate.policy().getPolicyId()))
                        .reversed()
                        .thenComparing(
                                Comparator.comparingInt(AiAssistantCandidate::lexicalScore).reversed()
                        ))
                .limit(Math.max(1, properties.getAssistantTopK()))
                .toList();
    }

    private String hash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("정책 임베딩 해시를 생성하지 못했습니다.", exception);
        }
    }

    private String writeVector(double[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("정책 임베딩을 저장 형식으로 변환하지 못했습니다.", exception);
        }
    }

    private double[] readVector(String vectorJson) {
        try {
            return objectMapper.readValue(vectorJson, double[].class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 정책 임베딩을 읽지 못했습니다.", exception);
        }
    }

    private double cosineSimilarity(double[] left, double[] right) {
        if (left.length != right.length || left.length == 0) return -1;

        double dotProduct = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < left.length; i++) {
            dotProduct += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) return -1;
        return dotProduct / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
