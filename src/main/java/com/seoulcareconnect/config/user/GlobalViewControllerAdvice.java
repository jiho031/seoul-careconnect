package com.seoulcareconnect.config.user;

import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class GlobalViewControllerAdvice {

    private final UserRepository userRepository;

    /**
     * 모든 Thymeleaf 화면에서 ${currentUiMode} 사용 가능
     */
    @ModelAttribute("currentUiMode")
    public String currentUiMode(Authentication authentication) {

        return findCurrentUser(authentication)
                .map(User::getUiMode)
                .filter(uiMode -> !uiMode.isBlank())
                .orElse("DEFAULT");
    }

    private Optional<User> findCurrentUser(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        /*
         * 일반 로그인 및 Remember-me 로그인
         */
        if (principal instanceof UserDetails userDetails) {

            String email = userDetails.getUsername()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            return userRepository.findByEmail(email);
        }

        /*
         * 구글·카카오 소셜 로그인
         */
        if (authentication instanceof OAuth2AuthenticationToken token) {

            String registrationId =
                    token.getAuthorizedClientRegistrationId();

            Map<String, Object> attributes =
                    token.getPrincipal().getAttributes();

            if ("google".equals(registrationId)) {

                String providerId =
                        String.valueOf(attributes.get("sub"));

                return userRepository.findByProviderAndProviderId(
                        "GOOGLE",
                        providerId
                );
            }

            if ("kakao".equals(registrationId)) {

                String providerId =
                        String.valueOf(attributes.get("id"));

                return userRepository.findByProviderAndProviderId(
                        "KAKAO",
                        providerId
                );
            }
        }

        return Optional.empty();
    }
}