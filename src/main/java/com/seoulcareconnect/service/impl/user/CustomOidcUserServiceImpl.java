package com.seoulcareconnect.service.impl.user;

import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.user.UserRepository;
import com.seoulcareconnect.service.user.CustomOidcUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomOidcUserServiceImpl implements CustomOidcUserService {

    private final UserRepository userRepository;

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
        // 이미 구글 계정으로 등록된 회원
        return userRepository
                .findByProviderAndProviderId(provider, providerId)
                .orElseGet(() ->
                        userRepository.findByEmail(email)
                                .map(existingUser -> {
                                    // 일반 회원가입 계정과 이메일이 같으면
                                    // 새 행을 만들지 않고 기존 회원에 구글 계정 연결
                                    existingUser.setProvider(provider);
                                    existingUser.setProviderId(providerId);

                                    return userRepository.save(existingUser);
                                })
                                .orElseGet(() -> {
                                    // 가입된 이메일이 없을 때만 신규 구글 회원 생성
                                    User newUser = new User();

                                    newUser.setEmail(email);
                                    newUser.setPassword(null);
                                    newUser.setName(name);
                                    newUser.setProvider(provider);
                                    newUser.setProviderId(providerId);
                                    newUser.setRole("USER");
                                    newUser.setRegion("서울");
                                    newUser.setUiMode("DEFAULT");
                                    newUser.setIsActive(true);

                                    return userRepository.save(newUser);
                                })
                );
    }
}