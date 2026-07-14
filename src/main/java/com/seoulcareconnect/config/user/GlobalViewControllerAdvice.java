package com.seoulcareconnect.config.user;

import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalViewControllerAdvice {

    private final UserRepository userRepository;

    @ModelAttribute("currentUiMode")
    public String currentUiMode(Authentication authentication) {

        Optional<User> currentUser = findCurrentUser(authentication);

        if (currentUser.isEmpty()) {
            return "DEFAULT";
        }

        String uiMode = currentUser.get().getUiMode();

        if (uiMode == null || uiMode.isBlank()) {
            return "DEFAULT";
        }

        return uiMode.trim();
    }

    private Optional<User> findCurrentUser(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {

            String provider = oauthToken
                    .getAuthorizedClientRegistrationId()
                    .toUpperCase(Locale.ROOT);

            Map<String, Object> attributes =
                    oauthToken.getPrincipal().getAttributes();

            String providerId;

            if ("GOOGLE".equals(provider)) {
                providerId = String.valueOf(attributes.get("sub"));
            } else if ("KAKAO".equals(provider)) {
                providerId = String.valueOf(attributes.get("id"));
            } else {
                return Optional.empty();
            }

            Optional<User> socialUser =
                    userRepository.findByProviderAndProviderId(
                            provider,
                            providerId
                    );

            if (socialUser.isPresent()) {
                return socialUser;
            }

            // provider 정보가 덮어써진 경우 이메일로 한 번 더 확인
            String email = extractEmail(attributes);

            if (email != null && !email.isBlank()) {
                return userRepository.findByEmail(
                        email.trim().toLowerCase(Locale.ROOT)
                );
            }

            return Optional.empty();
        }

        // 일반 로그인 및 Remember-me 로그인
        String email = authentication.getName()
                .trim()
                .toLowerCase(Locale.ROOT);

        return userRepository.findByEmail(email);
    }

    private String extractEmail(Map<String, Object> attributes) {

        Object email = attributes.get("email");

        if (email != null) {
            return String.valueOf(email);
        }

        Object kakaoAccountObject = attributes.get("kakao_account");

        if (kakaoAccountObject instanceof Map<?, ?> kakaoAccount) {
            Object kakaoEmail = kakaoAccount.get("email");

            if (kakaoEmail != null) {
                return String.valueOf(kakaoEmail);
            }
        }

        return null;
    }
}