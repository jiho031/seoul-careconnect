// 실제 프로젝트 경로: src/main/java/com/seoulcareconnect/controller/report/MissingPolicyReportController.java
// [수정] PolicyController와 동일하게 Authentication으로 로그인 유저를 직접 조회하도록 변경.
// hidden input으로 userId를 클라이언트에서 받던 방식은 제거(보안상 더 안전).
package com.seoulcareconnect.controller.report;

import com.seoulcareconnect.dto.report.MissingPolicyReportRequestDto;
import com.seoulcareconnect.entity.user.User;
import com.seoulcareconnect.repository.user.UserRepository;
import com.seoulcareconnect.service.report.MissingPolicyReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reports")
public class MissingPolicyReportController {

    private final MissingPolicyReportService missingPolicyReportService;
    private final UserRepository userRepository;

    @GetMapping
    public String form(Model model, Authentication authentication) {
        if (!model.containsAttribute("reportRequest")) {
            model.addAttribute("reportRequest", new MissingPolicyReportRequestDto());
        }
        model.addAttribute("user", resolveCurrentUserOrNull(authentication));
        return "report/form";
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("reportRequest") MissingPolicyReportRequestDto requestDto,
                         BindingResult bindingResult,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {

        User user = resolveCurrentUserOrNull(authentication);
        if (user == null) {
            redirectAttributes.addFlashAttribute("reportError", "로그인 후 이용 가능합니다.");
            return "redirect:/login"; // 실제 로그인 페이지 경로에 맞게 수정하세요.
        }

        if (bindingResult.hasErrors()) {
            return "report/form";
        }

        requestDto.setUserId(user.getUserId());
        missingPolicyReportService.submitReport(requestDto);

        redirectAttributes.addFlashAttribute("reportSuccess", "신고가 접수되었습니다. 확인 후 반영해드릴게요.");
        return "redirect:/reports";
    }

    // 아래 3개 메서드는 PolicyController.resolveCurrentUser와 동일한 로직입니다.
    // (프로젝트에 공용 유틸/베이스 컨트롤러가 없어서 그대로 복제했습니다.
    //  나중에 여러 컨트롤러에서 반복되면 공용 클래스로 뽑는 걸 추천드려요.)
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
}
