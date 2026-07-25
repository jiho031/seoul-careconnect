package com.seoulcareconnect.config.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private boolean enabled = false;
    private String apiKey = "";
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-5.6";
    private String promptVersion = "policy-easy-v1";
    private int maxOutputTokens = 1200;
    private String embeddingModel = "text-embedding-3-small";
    private int embeddingDimensions = 512;
    private int assistantCandidateLimit = 80;
    private int assistantTopK = 5;
    private int assistantMaxOutputTokens = 900;
}
