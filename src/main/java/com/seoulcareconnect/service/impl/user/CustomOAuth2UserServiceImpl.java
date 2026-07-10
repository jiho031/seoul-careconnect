package com.seoulcareconnect.service.impl.user;

import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.user.UserRepository;
import com.seoulcareconnect.service.user.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserServiceImpl implements CustomOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        String registrationId =
                userRequest.getClientRegistration().getRegistrationId();

        OAuthUserInfo userInfo = extractUserInfo(
                registrationId,
                oauth2User.getAttributes()
        );

        User user = findOrCreateUser(userInfo);

        return new DefaultOAuth2User(
                Collections.singleton(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole())
                ),
                oauth2User.getAttributes(),
                userInfo.nameAttributeKey()
        );
    }

    private User findOrCreateUser(OAuthUserInfo info) {

        Optional<User> existingByProvider =
                userRepository.findByProviderAndProviderId(
                        info.provider(),
                        info.providerId()
                );

        if (existingByProvider.isPresent()) {
            return existingByProvider.get();
        }

        if (info.email() != null) {
            Optional<User> existingByEmail =
                    userRepository.findByEmail(info.email());

            if (existingByEmail.isPresent()) {
                User user = existingByEmail.get();

                user.setProvider(info.provider());
                user.setProviderId(info.providerId());

                return userRepository.save(user);
            }
        }

        User user = new User();

        user.setEmail(info.email());

        // 소셜 로그인 계정 전용 임의 비밀번호
        user.setPassword(
                passwordEncoder.encode(UUID.randomUUID().toString())
        );

        user.setName(info.name());
        user.setProvider(info.provider());
        user.setProviderId(info.providerId());
        user.setRole("USER");
        user.setRegion("서울");
        user.setUiMode("DEFAULT");
        user.setIsActive(true);

        return userRepository.save(user);
    }

    @SuppressWarnings("unchecked")
    private OAuthUserInfo extractUserInfo(
            String registrationId,
            Map<String, Object> attributes
    ) {

        if ("google".equals(registrationId)) {
            return new OAuthUserInfo(
                    "GOOGLE",
                    String.valueOf(attributes.get("sub")),
                    (String) attributes.get("email"),
                    (String) attributes.getOrDefault("name", "구글 사용자"),
                    "sub"
            );
        }

        if ("kakao".equals(registrationId)) {
            String providerId =
                    String.valueOf(attributes.get("id"));

            Map<String, Object> kakaoAccount =
                    (Map<String, Object>) attributes.get("kakao_account");

            Map<String, Object> profile =
                    kakaoAccount == null
                            ? null
                            : (Map<String, Object>) kakaoAccount.get("profile");

            String email = kakaoAccount == null
                    ? null
                    : (String) kakaoAccount.get("email");

            if (email == null || email.isBlank()) {
                email = "kakao_" + providerId + "@oauth.local";
            } else {
                email = email.trim().toLowerCase();
            }

            String name = profile == null
                    ? "카카오 사용자"
                    : String.valueOf(
                    profile.getOrDefault(
                            "nickname",
                            "카카오 사용자"
                    )
            );

            return new OAuthUserInfo(
                    "KAKAO",
                    providerId,
                    email,
                    name,
                    "id"
            );
        }

        throw new OAuth2AuthenticationException(
                "지원하지 않는 소셜 로그인입니다."
        );
    }

    private record OAuthUserInfo(
            String provider,
            String providerId,
            String email,
            String name,
            String nameAttributeKey
    ) {
    }
}