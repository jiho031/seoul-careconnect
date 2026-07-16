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
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.*;

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

        // 동일한 카카오 계정으로 이미 가입된 회원
        Optional<User> existingByProvider =
                userRepository.findByProviderAndProviderId(
                        info.provider(),
                        info.providerId()
                );

        if (existingByProvider.isPresent()) {
            User user = existingByProvider.get();

            validateActiveUser(user);

            return user;
        }

        // 카카오에서 이메일을 받은 경우 동일 이메일 확인
        if (info.email() != null && !info.email().isBlank()) {

            String email = info.email()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            Optional<User> existingByEmail =
                    userRepository.findByEmail(email);

            if (existingByEmail.isPresent()) {
                User user = existingByEmail.get();

                validateActiveUser(user);

                String existingProvider =
                        normalizeProvider(user.getProvider());

                /*
                 * 기존 가입 방식이 카카오가 아니면
                 * provider를 덮어쓰지 않고 로그인 차단
                 */
                if (!info.provider().equals(existingProvider)) {
                    throw providerConflict(existingProvider);
                }

                /*
                 * 과거 데이터에 카카오 providerId만 없는 경우
                 * 한 번만 연결
                 */
                if (user.getProviderId() == null
                        || user.getProviderId().isBlank()) {

                    user.setProviderId(info.providerId());

                    return userRepository.save(user);
                }

                /*
                 * 같은 이메일이지만 카카오 사용자 ID가 다름
                 */
                throw providerConflict(existingProvider);
            }
        }

        // 완전 신규 카카오 회원
        User user = new User();

        String email = info.email();

        /*
         * 카카오 이메일을 받지 못했다면
         * providerId를 이용해 내부 이메일 생성
         */
        if (email == null || email.isBlank()) {
            email = "kakao_"
                    + info.providerId()
                    + "@oauth.local";
        } else {
            email = email.trim().toLowerCase(Locale.ROOT);
        }

        user.setEmail(email);
        user.setPassword(
                passwordEncoder.encode(
                        UUID.randomUUID().toString()
                )
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

    private String normalizeProvider(String provider) {

        if (provider == null || provider.isBlank()) {
            return "LOCAL";
        }

        return provider.trim().toUpperCase(Locale.ROOT);
    }

    private OAuth2AuthenticationException providerConflict(
            String existingProvider
    ) {
        String loginMethod = switch (existingProvider) {
            case "GOOGLE" -> "구글 로그인";
            case "KAKAO" -> "카카오 로그인";
            default -> "이메일과 비밀번호 로그인";
        };

        return new OAuth2AuthenticationException(
                new OAuth2Error("provider_conflict"),
                "이미 " + loginMethod
                        + " 방식으로 가입된 이메일입니다. "
                        + "기존 로그인 방식을 이용해주세요."
        );
    }

    private void validateActiveUser(User user) {

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("inactive_user"),
                    "탈퇴하거나 비활성화된 계정입니다."
            );
        }
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