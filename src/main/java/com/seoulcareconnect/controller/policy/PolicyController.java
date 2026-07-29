package com.seoulcareconnect.controller.policy;

import com.seoulcareconnect.dto.policy.PolicyDTO;
import com.seoulcareconnect.dto.policy.PolicyDetailDTO;
import com.seoulcareconnect.dto.policy.PolicySearchDTO;
import com.seoulcareconnect.entity.policy.enums.AgeGroup;
import com.seoulcareconnect.entity.policy.enums.ApplyStatus;
import com.seoulcareconnect.entity.policy.enums.PolicyCategory;
import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.user.UserRepository;
import com.seoulcareconnect.service.ai.AiPolicyExplanationService;
import com.seoulcareconnect.service.policy.PolicyDetailService;
import com.seoulcareconnect.service.policy.PolicyService;
import com.seoulcareconnect.service.policy.PolicyViewLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PolicyController {

    private static final List<String> SEOUL_DISTRICTS = List.of(
            "강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구",
            "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구",
            "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구"
    );

    private static final List<String> TARGET_KEYWORDS = List.of(
            "구직자", "1인가구", "저소득층", "소상공인", "장애인", "돌봄가구"
    );

    private final PolicyService policyService;
    private final PolicyDetailService policyDetailService;
    private final PolicyViewLogService policyViewLogService;
    private final UserRepository userRepository;
    private final AiPolicyExplanationService aiPolicyExplanationService;

    @GetMapping("/policies")
    public String list(
            @ModelAttribute("search") PolicySearchDTO search,
            Authentication authentication,
            Model model
    ) {
        Page<PolicyDTO> policyPage = "recent".equals(search.safeQuick())
                ? policyService.recentViewed(
                resolveCurrentUser(authentication).getUserId(),
                search.safePage(),
                search.safeSize(4, 20)
        )
                : policyService.search(search);

        model.addAttribute("policyPage", policyPage);
        model.addAttribute("user", resolveCurrentUserOrNull(authentication));
        model.addAttribute("pageNumbers", pageNumbers(policyPage));
        model.addAttribute("districts", SEOUL_DISTRICTS);
        model.addAttribute("ageGroups", AgeGroup.values());
        model.addAttribute("categories", PolicyCategory.values());
        model.addAttribute("targetKeywords", TARGET_KEYWORDS);
        model.addAttribute("applyStatuses", List.of(
                ApplyStatus.OPEN,
                ApplyStatus.CLOSING_SOON,
                ApplyStatus.ALWAYS
        ));
        return "policy/list";
    }

    @GetMapping("/policies/personalized")
    public String personalizedSearch(
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        User user = resolveCurrentUser(authentication);

        if (user.getDistrict() != null && !user.getDistrict().isBlank()) {
            redirectAttributes.addAttribute("district", user.getDistrict().trim());
        }

        String ageGroup = toSearchAgeGroup(user.getAgeGroup());
        if (ageGroup != null) {
            redirectAttributes.addAttribute("ageGroup", ageGroup);
        }

        redirectAttributes.addAttribute("page", 0);
        return "redirect:/policies";
    }

    @GetMapping("/policies/{policyId:\\d+}")
    public String detail(
            @PathVariable Long policyId,
            Authentication authentication,
            HttpServletRequest request,
            Model model
    ) {
        PolicyDetailDTO policy = policyDetailService.get(policyId);
        User currentUser = resolveCurrentUser(authentication);

        policyViewLogService.record(
                policyId,
                currentUser.getUserId(),
                request.getHeader("User-Agent")
        );

        model.addAttribute("policy", policy);
        model.addAttribute("user", currentUser);
        model.addAttribute("aiAssistantPolicyId", policyId);
        model.addAttribute("aiAssistantPolicyTitle", policy.getTitle());
        model.addAttribute(
                "aiExplanation",
                aiPolicyExplanationService.findApproved(policyId).orElse(null)
        );
        return "policy/detail";
    }

    private List<Integer> pageNumbers(Page<?> page) {
        if (page.getTotalPages() == 0) return List.of();

        int start = Math.max(0, page.getNumber() - 2);
        int end = Math.min(page.getTotalPages() - 1, start + 4);
        start = Math.max(0, end - 4);

        return java.util.stream.IntStream.rangeClosed(start, end)
                .boxed()
                .toList();
    }

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

    private User resolveCurrentUserOrNull(Authentication authentication) {
        try {
            return resolveCurrentUser(authentication);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalStateException("로그인 회원을 찾을 수 없습니다."));
    }

    private String toSearchAgeGroup(String userAgeGroup) {
        if (userAgeGroup == null || userAgeGroup.isBlank()) {
            return null;
        }

        return switch (userAgeGroup.trim()) {
            case "20대 이하", "30대", "30대 이하" -> AgeGroup.UNDER_40.name();
            case "40대" -> AgeGroup.FORTIES.name();
            case "50대" -> AgeGroup.FIFTIES.name();
            case "60대 이상" -> AgeGroup.SIXTIES_PLUS.name();
            default -> null;
        };
    }
}
