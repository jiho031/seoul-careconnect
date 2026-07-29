package com.seoulcareconnect.controller.user;

import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.user.UserRepository;
import com.seoulcareconnect.service.user.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class FavoritePageController {

    private static final int RECOMMEND_LIMIT = 6;

    private final FavoriteService favoriteService;
    private final UserRepository userRepository;

    @GetMapping("/favorites")
    public String favoritesPage(Authentication authentication, Model model) {
        User user = resolveCurrentUser(authentication);

        model.addAttribute("user", user);
        model.addAttribute("favorites", favoriteService.getFavoriteCards(user.getUserId()));
        model.addAttribute("recommendedPolicies",
                favoriteService.getRecommendedPolicies(user.getUserId(), RECOMMEND_LIMIT));

        return "favorite/list";
    }

    // PolicyController와 동일한 사용자 식별 로직
    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof OidcUser oidcUser) {
            String email = oidcUser.getEmail();
            if (email != null && !email.isBlank()) return findByEmail(email);
        }

        if (principal instanceof OAuth2User oauth2User) {
            Map<String, Object> attributes = oauth2User.getAttributes();

            Object directEmail = attributes.get("email");
            if (directEmail instanceof String email && !email.isBlank()) {
                return findByEmail(email);
            }

            Object kakaoAccountObject = attributes.get("kakao_account");
            if (kakaoAccountObject instanceof Map<?, ?> kakaoAccount) {
                Object nestedEmail = kakaoAccount.get("email");
                if (nestedEmail instanceof String email && !email.isBlank()) {
                    return findByEmail(email);
                }
            }

            Object providerId = attributes.get("id");
            if (providerId != null) {
                return userRepository.findByProviderAndProviderId(
                                "KAKAO",
                                String.valueOf(providerId)
                        )
                        .orElseThrow(() -> new IllegalStateException("로그인 회원을 찾을 수 없습니다."));
            }
        }

        if (principal instanceof UserDetails userDetails) {
            return findByEmail(userDetails.getUsername());
        }

        return findByEmail(authentication.getName());
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalStateException("로그인 회원을 찾을 수 없습니다."));
    }
}