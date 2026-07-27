package com.seoulcareconnect.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulcareconnect.config.ai.AiProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AiModelGatewayTest {

    private HttpServer server;
    private final AtomicInteger openAiCalls = new AtomicInteger();
    private final AtomicInteger ollamaCalls = new AtomicInteger();
    private final AtomicReference<String> ollamaEmbedContentType = new AtomicReference<>();
    private final AtomicReference<String> ollamaEmbedRequestBody = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/responses", this::openAiFailure);
        server.createContext("/v1/responses", this::openAiFailure);
        server.createContext("/api/chat", this::ollamaSuccess);
        server.createContext("/api/embed", this::ollamaEmbedSuccess);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void fallsBackToOllamaWhenOpenAiFails() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/v1");
        properties.setOllamaBaseUrl("http://localhost:" + server.getAddress().getPort());

        AiModelGateway gateway = new AiModelGateway(new ObjectMapper(), properties);
        AiModelGateway.GeneratedJson result = gateway.generateJson(
                "test_answer",
                "정책 근거만 사용하세요.",
                List.of(new AiModelGateway.Message("user", "질문입니다.")),
                Map.of(
                        "type", "object",
                        "properties", Map.of("answer", Map.of("type", "string")),
                        "required", List.of("answer")
                ),
                100,
                List.of("answer")
        );

        assertThat(result.provider()).isEqualTo(AiModelGateway.Provider.OLLAMA);
        assertThat(result.payload().path("answer").asText()).isEqualTo("Ollama 답변");
        assertThat(openAiCalls.get()).isEqualTo(1);
        assertThat(ollamaCalls.get()).isEqualTo(1);
    }

    @Test
    void sendsJsonEmbeddingRequestToOllama() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setOllamaBaseUrl("http://localhost:" + server.getAddress().getPort());

        AiModelGateway gateway = new AiModelGateway(new ObjectMapper(), properties);
        AiModelGateway.EmbeddingBatch result = gateway.embed(List.of("서울시 중장년 일자리 정책"));

        assertThat(result.vectors()).hasSize(1);
        assertThat(ollamaEmbedContentType.get()).startsWith("application/json");
        assertThat(ollamaEmbedRequestBody.get())
                .contains("\"model\":\"embeddinggemma\"")
                .contains("\"input\":[\"서울시 중장년 일자리 정책\"]");
    }

    private void openAiFailure(HttpExchange exchange) throws IOException {
        openAiCalls.incrementAndGet();
        writeJson(exchange, 503, "{\"error\":\"temporary\"}");
    }

    private void ollamaSuccess(HttpExchange exchange) throws IOException {
        ollamaCalls.incrementAndGet();
        writeJson(exchange, 200, "{\"message\":{\"content\":\"{\\\"answer\\\":\\\"Ollama 답변\\\"}\"}}");
    }

    private void ollamaEmbedSuccess(HttpExchange exchange) throws IOException {
        ollamaEmbedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
        ollamaEmbedRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        writeJson(exchange, 200, "{\"embeddings\":[[0.1,0.2]]}");
    }

    private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
