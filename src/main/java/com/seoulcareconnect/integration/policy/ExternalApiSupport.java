package com.seoulcareconnect.integration.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class ExternalApiSupport {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;

    public ExternalApiSupport(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.xmlMapper = new XmlMapper();
    }

    public String get(String url, Map<String, ?> params) {
        URI uri = buildUri(url, params);

        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

        } catch (RestClientResponseException e) {
            String responseBody = preview(
                    e.getResponseBodyAsString()
            );

            throw new IllegalStateException(
                    "외부 API 호출 실패: HTTP "
                            + e.getStatusCode().value()
                            + ", 응답="
                            + responseBody,
                    e
            );

        } catch (ResourceAccessException e) {
            Throwable cause = e.getMostSpecificCause();

            String causeMessage =
                    cause == null
                            ? e.getMessage()
                            : cause.getClass().getSimpleName()
                            + ": "
                            + cause.getMessage();

            throw new IllegalStateException(
                    "외부 API 연결 실패: " + causeMessage,
                    e
            );
        }
    }

    public JsonNode readJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 응답 파싱 실패", e);
        }
    }

    public JsonNode readXml(String body) {
        try {
            return xmlMapper.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("XML 응답 파싱 실패", e);
        }
    }

    private URI buildUri(String url, Map<String, ?> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);

        params.forEach((name, rawValue) -> {
            if (rawValue == null) return;
            String value = rawValue.toString().trim();
            if (value.isBlank()) return;
            builder.queryParam(name, isKeyParameter(name) ? decodeKey(value) : value);
        });

        return builder.build().encode(StandardCharsets.UTF_8).toUri();
    }

    private boolean isKeyParameter(String name) {
        String lower = name.toLowerCase();
        return lower.contains("servicekey")
                || lower.contains("accesskey")
                || lower.contains("authkey")
                || lower.contains("crtfckey");
    }

    private String decodeKey(String key) {
        if (!key.contains("%")) return key;

        try {
            return URLDecoder.decode(key, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return key;
        }
    }

    private String preview(String value) {
        if (value == null || value.isBlank()) {
            return "(응답 내용 없음)";
        }

        String normalized = value
                .replaceAll("\\s+", " ")
                .trim();

        return normalized.substring(
                0,
                Math.min(normalized.length(), 1000)
        );
    }
}
