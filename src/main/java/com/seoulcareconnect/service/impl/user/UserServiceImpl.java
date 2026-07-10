package com.seoulcareconnect.service.impl.user;

import com.seoulcareconnect.dto.user.MyPageResponse;
import com.seoulcareconnect.dto.user.PasswordChangeRequest;
import com.seoulcareconnect.dto.user.SignupRequest;
import com.seoulcareconnect.dto.user.UserUpdateRequest;
import com.seoulcareconnect.dto.user.WithdrawRequest;
import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.user.UserRepository;
import com.seoulcareconnect.service.user.EmailVerifyService;
import com.seoulcareconnect.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final Set<String> AGE_GROUPS = Set.of(
            "20대 이하", "30대", "40대", "50대", "60대 이상"
    );

    private static final Set<String> UI_MODES = Set.of(
            "DEFAULT", "LARGE_TEXT"
    );

    private static final Set<String> SEOUL_DISTRICTS = Set.of(
            "강남구", "강동구", "강북구", "강서구", "관악구",
            "광진구", "구로구", "금천구", "노원구", "도봉구",
            "동대문구", "동작구", "마포구", "서대문구", "서초구",
            "성동구", "성북구", "송파구", "양천구", "영등포구",
            "용산구", "은평구", "종로구", "중구", "중랑구"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerifyService emailVerifyService;

    @Override
    @Transactional
    public void signup(SignupRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        if (!emailVerifyService.isEmailVerified(email)) {
            throw new IllegalArgumentException("이메일 인증을 완료해주세요.");
        }

        User user = new User();
        user.setBirthYear(toNullIfBlank(request.getBirthYear()));
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName().trim());
        user.setPhone(toNullIfBlank(request.getPhone()));
        user.setAgeGroup(request.getAgeGroup());
        user.setDistrict(request.getDistrict());
        user.setRole("USER");
        user.setRegion("서울");
        user.setProvider("LOCAL");

        if ("60대 이상".equals(request.getAgeGroup())) {
            user.setUiMode("LARGE_TEXT");
        } else {
            user.setUiMode("DEFAULT");
        }

        userRepository.save(user);
    }

    @Override
    public MyPageResponse getMyPage(Authentication authentication) {
        return MyPageResponse.from(getCurrentUser(authentication));
    }

    @Override
    @Transactional
    public void updateMyPage(
            Authentication authentication,
            UserUpdateRequest request
    ) {
        User user = getCurrentUser(authentication);

        String name = request.getName() == null
                ? ""
                : request.getName().trim();

        if (name.isBlank()) {
            throw new IllegalArgumentException("이름을 입력해주세요.");
        }

        if (!AGE_GROUPS.contains(request.getAgeGroup())) {
            throw new IllegalArgumentException("올바른 연령대를 선택해주세요.");
        }

        if (!SEOUL_DISTRICTS.contains(request.getDistrict())) {
            throw new IllegalArgumentException("올바른 관심 지역을 선택해주세요.");
        }

        if (!UI_MODES.contains(request.getUiMode())) {
            throw new IllegalArgumentException("올바른 화면 모드를 선택해주세요.");
        }

        user.setName(name);
        user.setBirthYear(toNullIfBlank(request.getBirthYear()));
        user.setPhone(toNullIfBlank(request.getPhone()));
        user.setAgeGroup(request.getAgeGroup());
        user.setDistrict(request.getDistrict());
        user.setUiMode(request.getUiMode());

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(
            Authentication authentication,
            PasswordChangeRequest request
    ) {
        User user = getCurrentUser(authentication);

        if (!"LOCAL".equalsIgnoreCase(user.getProvider())) {
            throw new IllegalArgumentException(
                    "소셜 로그인 계정은 비밀번호를 변경할 수 없습니다."
            );
        }

        if (user.getPassword() == null ||
                !passwordEncoder.matches(
                        request.getCurrentPassword(),
                        user.getPassword()
                )) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        if (!request.getNewPassword()
                .equals(request.getNewPasswordConfirm())) {
            throw new IllegalArgumentException("새 비밀번호 확인이 일치하지 않습니다.");
        }

        if (passwordEncoder.matches(
                request.getNewPassword(),
                user.getPassword()
        )) {
            throw new IllegalArgumentException(
                    "현재 비밀번호와 다른 비밀번호를 입력해주세요."
            );
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void withdraw(
            Authentication authentication,
            WithdrawRequest request
    ) {
        User user = getCurrentUser(authentication);

        if (!"탈퇴합니다".equals(request.getConfirmText())) {
            throw new IllegalArgumentException(
                    "확인 문구에 ‘탈퇴합니다’를 정확히 입력해주세요."
            );
        }

        if ("LOCAL".equalsIgnoreCase(user.getProvider())) {
            if (request.getCurrentPassword() == null ||
                    !passwordEncoder.matches(
                            request.getCurrentPassword(),
                            user.getPassword()
                    )) {
                throw new IllegalArgumentException(
                        "현재 비밀번호가 일치하지 않습니다."
                );
            }
        }

        user.setIsActive(false);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String provider = oauthToken
                    .getAuthorizedClientRegistrationId()
                    .toUpperCase(Locale.ROOT);

            String providerId = oauthToken.getPrincipal().getName();

            return userRepository
                    .findByProviderAndProviderId(provider, providerId)
                    .orElseGet(() -> findSocialUserByEmail(oauthToken));
        }

        return userRepository
                .findByEmail(normalizeEmail(authentication.getName()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "회원 정보를 찾을 수 없습니다."
                ));
    }

    @SuppressWarnings("unchecked")
    private User findSocialUserByEmail(
            OAuth2AuthenticationToken oauthToken
    ) {
        Map<String, Object> attributes =
                oauthToken.getPrincipal().getAttributes();

        String email = null;

        Object directEmail = attributes.get("email");
        if (directEmail != null) {
            email = String.valueOf(directEmail);
        }

        if (email == null && attributes.get("kakao_account") instanceof Map) {
            Map<String, Object> kakaoAccount =
                    (Map<String, Object>) attributes.get("kakao_account");

            if (kakaoAccount.get("email") != null) {
                email = String.valueOf(kakaoAccount.get("email"));
            }
        }

        if (email != null && !email.isBlank()) {
            return userRepository
                    .findByEmail(normalizeEmail(email))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "소셜 로그인 회원 정보를 찾을 수 없습니다."
                    ));
        }

        throw new IllegalArgumentException(
                "소셜 로그인 회원 정보를 찾을 수 없습니다."
        );
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String toNullIfBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
