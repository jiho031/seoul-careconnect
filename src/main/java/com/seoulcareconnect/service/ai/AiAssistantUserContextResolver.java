package com.seoulcareconnect.service.ai;

import com.seoulcareconnect.dto.ai.AiAssistantUserContext;
import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AiAssistantUserContextResolver {

    private final UserRepository userRepository;

    public AiAssistantUserContext resolve(Authentication authentication) {
        return findUser(authentication)
                .map(user -> new AiAssistantUserContext(
                        user.getDistrict(),
                        user.getAgeGroup()
                ))
                .orElseGet(AiAssistantUserContext::empty);
    }

    private Optional<User> findUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String provider = oauthToken.getAuthorizedClientRegistrationId()
                    .toUpperCase(Locale.ROOT);
            Map<String, Object> attributes = oauthToken.getPrincipal().getAttributes();
            Object providerId = "GOOGLE".equals(provider)
                    ? attributes.get("sub")
                    : attributes.get("id");

            if (providerId != null) {
                Optional<User> socialUser = userRepository.findByProviderAndProviderId(
                        provider,
                        String.valueOf(providerId)
                );
                if (socialUser.isPresent()) return socialUser;
            }

            String email = extractEmail(attributes);
            if (email != null) {
                return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT));
            }
            return Optional.empty();
        }

        return userRepository.findByEmail(
                authentication.getName().trim().toLowerCase(Locale.ROOT)
        );
    }

    private String extractEmail(Map<String, Object> attributes) {
        Object email = attributes.get("email");
        if (email != null) return String.valueOf(email);

        Object kakaoAccountObject = attributes.get("kakao_account");
        if (kakaoAccountObject instanceof Map<?, ?> kakaoAccount) {
            Object kakaoEmail = kakaoAccount.get("email");
            if (kakaoEmail != null) return String.valueOf(kakaoEmail);
        }
        return null;
    }
}
