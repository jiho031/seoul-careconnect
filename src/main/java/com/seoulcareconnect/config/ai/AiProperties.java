package com.seoulcareconnect.config.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private String apiKey = environmentValue("OPENAI_API_KEY");
    private boolean enabled = !apiKey.isBlank();
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-5.6";
    private String promptVersion = "policy-easy-v1";
    private int maxOutputTokens = 1200;
    private boolean policyExtractionEnabled = true;
    private String policyExtractionPromptVersion = "policy-application-extract-v1";
    private int policyExtractionMaxOutputTokens = 1800;
    private String embeddingModel = "text-embedding-3-small";
    private int embeddingDimensions = 512;
    private int assistantCandidateLimit = 80;
    private int assistantTopK = 5;
    private int assistantMaxOutputTokens = 900;
    private boolean ollamaEnabled = true;
    private String ollamaBaseUrl = "http://localhost:11434";
    private String ollamaModel = "llama3.2";
    private String ollamaEmbeddingModel = "embeddinggemma";
    private int ollamaEmbeddingDimensions = 0;

    public String resolvedApiKey() {
        return apiKey == null || apiKey.isBlank()
                ? environmentValue("OPENAI_API_KEY")
                : apiKey.trim();
    }

    private static String environmentValue(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }
}
