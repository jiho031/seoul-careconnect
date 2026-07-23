package com.seoulcareconnect.config.user;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Arrays;

@ControllerAdvice
public class GlobalViewAdvice {

    private static final String REMEMBER_ME_COOKIE =
            "seoul-careconnect-remember-me";

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
}