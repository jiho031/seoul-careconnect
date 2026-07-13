package com.seoulcareconnect.config.user;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.HashMap;
import java.util.Map;

public class CustomAuthorizationRequestResolver
        implements OAuth2AuthorizationRequestResolver {

    private static final String AUTHORIZATION_REQUEST_BASE_URI =
            "/oauth2/authorization";

    private final OAuth2AuthorizationRequestResolver defaultResolver;

    public CustomAuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository
    ) {
        this.defaultResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository,
                        AUTHORIZATION_REQUEST_BASE_URI
                );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(
            HttpServletRequest request
    ) {
        OAuth2AuthorizationRequest authorizationRequest =
                defaultResolver.resolve(request);

        return customizeAuthorizationRequest(
                request,
                authorizationRequest
        );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(
            HttpServletRequest request,
            String clientRegistrationId
    ) {
        OAuth2AuthorizationRequest authorizationRequest =
                defaultResolver.resolve(
                        request,
                        clientRegistrationId
                );

        return customizeAuthorizationRequest(
                request,
                authorizationRequest
        );
    }

    private OAuth2AuthorizationRequest customizeAuthorizationRequest(
            HttpServletRequest request,
            OAuth2AuthorizationRequest authorizationRequest
    ) {
        if (authorizationRequest == null) {
            return null;
        }

        Map<String, Object> additionalParameters =
                new HashMap<>(
                        authorizationRequest.getAdditionalParameters()
                );

        String requestUri = request.getRequestURI();

        if (requestUri.endsWith("/google")) {
            // 구글 계정 선택 화면 표시
            additionalParameters.put(
                    "prompt",
                    "select_account"
            );
        }

        if (requestUri.endsWith("/kakao")) {
            // 카카오 계정 선택 화면 표시
            additionalParameters.put(
                    "prompt",
                    "select_account"
            );
        }

        return OAuth2AuthorizationRequest
                .from(authorizationRequest)
                .additionalParameters(additionalParameters)
                .build();
    }
}