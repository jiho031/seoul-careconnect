package com.seoulcareconnect.service.impl.user;

import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.user.UserRepository;
import com.seoulcareconnect.service.user.CustomOidcUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomOidcUserServiceImpl
        implements CustomOidcUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest)
            throws OAuth2AuthenticationException {

        OidcUserService delegate = new OidcUserService();
        OidcUser oidcUser = delegate.loadUser(userRequest);

        String registrationId = userRequest
                .getClientRegistration()
                .getRegistrationId();

        if (!"google".equals(registrationId)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider"),
                    "지원하지 않는 OIDC 로그인입니다."
            );
        }

        String provider = "GOOGLE";
        String providerId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"),
                    "구글 계정 이메일을 가져올 수 없습니다."
            );
        }

        email = email.trim().toLowerCase(Locale.ROOT);

        if (name == null || name.isBlank()) {
            name = "구글 사용자";
        }

        User user = findOrCreateGoogleUser(
                provider,
                providerId,
                email,
                name
        );

        // 탈퇴 또는 비활성 계정 로그인 차단
        validateActiveUser(user);

        return new DefaultOidcUser(
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole()
                        )
                ),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "sub"
        );
    }

    private User findOrCreateGoogleUser(
            String provider,
            String providerId,
            String email,
            String name
    ) {
        // 동일한 구글 계정으로 이미 가입된 회원
        Optional<User> existingByProvider =
                userRepository.findByProviderAndProviderId(
                        provider,
                        providerId
                );

        if (existingByProvider.isPresent()) {
            User user = existingByProvider.get();

            validateActiveUser(user);

            return user;
        }

        // 동일 이메일로 가입된 회원 확인
        Optional<User> existingByEmail =
                userRepository.findByEmail(email);

        if (existingByEmail.isPresent()) {
            User user = existingByEmail.get();

            validateActiveUser(user);

            String existingProvider =
                    normalizeProvider(user.getProvider());

            /*
             * 같은 이메일이지만 가입 방식이 다르면
             * 기존 provider를 덮어쓰지 않고 로그인 차단
             */
            if (!provider.equals(existingProvider)) {
                throw providerConflict(existingProvider);
            }

            /*
             * 같은 GOOGLE 계정인데 과거 데이터에
             * providerId만 비어 있는 경우 한 번만 연결
             */
            if (user.getProviderId() == null
                    || user.getProviderId().isBlank()) {

                user.setProviderId(providerId);

                return userRepository.save(user);
            }

            /*
             * 이메일과 provider는 같지만 providerId가 다르면
             * 다른 구글 계정이므로 연결하지 않음
             */
            throw providerConflict(existingProvider);
        }

        // 완전 신규 구글 회원
        User newUser = new User();

        newUser.setEmail(email);
        newUser.setPassword(
                passwordEncoder.encode(
                        UUID.randomUUID().toString()
                )
        );
        newUser.setName(name);
        newUser.setProvider(provider);
        newUser.setProviderId(providerId);
        newUser.setRole("USER");
        newUser.setRegion("서울");
        newUser.setUiMode("DEFAULT");
        newUser.setIsActive(true);

        return userRepository.save(newUser);
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
}