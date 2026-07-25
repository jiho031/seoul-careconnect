package com.seoulcareconnect.config.user;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Arrays;
import java.util.Set;

@ControllerAdvice
public class GlobalViewAdvice {

    private static final String REMEMBER_ME_COOKIE =
            "seoul-careconnect-remember-me";
    private static final Set<String> AI_ASSISTANT_PATHS = Set.of(
            "/",
            "/policies",
            "/favorites",
            "/guides",
            "/reports",
            "/mypage"
    );

    @ModelAttribute("rememberMeEnabled")
    public boolean rememberMeEnabled(
            HttpServletRequest request
    ) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return false;
        }

        return Arrays.stream(cookies)
                .anyMatch(cookie ->
                        REMEMBER_ME_COOKIE.equals(
                                cookie.getName()
                        )
                                && cookie.getValue() != null
                                && !cookie.getValue().isBlank()
                );
    }

    @ModelAttribute("aiAssistantVisible")
    public boolean aiAssistantVisible(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        String path = contextPath.isEmpty()
                ? requestUri
                : requestUri.substring(contextPath.length());

        return AI_ASSISTANT_PATHS.stream()
                .anyMatch(allowed -> "/".equals(allowed)
                        ? "/".equals(path)
                        : path.equals(allowed) || path.startsWith(allowed + "/"));
    }
}
