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
        // 1. 이미 해당 구글 계정으로 가입한 회원 확인
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

        // 2. 동일 이메일로 가입한 기존 회원 확인
        Optional<User> existingByEmail =
                userRepository.findByEmail(email);

        if (existingByEmail.isPresent()) {
            User user = existingByEmail.get();

            validateActiveUser(user);

            /*
             * 일반 회원가입 계정과 이메일이 같으면
             * 새 행을 만들지 않고 기존 회원에 구글 계정을 연결한다.
             */
            user.setProvider(provider);
            user.setProviderId(providerId);

            return userRepository.save(user);
        }

        // 3. 기존 회원이 없을 때 신규 구글 회원 생성
        User newUser = new User();

        newUser.setEmail(email);

        /*
         * 소셜 회원은 실제로 사용할 비밀번호가 없지만
         * DB password 컬럼이 NOT NULL이므로 임의 비밀번호를 저장한다.
         */
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

    private void validateActiveUser(User user) {

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("inactive_user"),
                    "탈퇴하거나 비활성화된 계정입니다."
            );
        }
    }
}