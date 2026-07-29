package com.seoulcareconnect.config.user;

import com.seoulcareconnect.dto.user.*;
import com.seoulcareconnect.service.user.FavoriteService;
import com.seoulcareconnect.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {

    private final UserService userService;
    private final FavoriteService favoriteService;

    @GetMapping
    public String myPage(Authentication authentication, Model model) {
        preparePage(model, authentication);
        return "user/mypage";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @Valid @ModelAttribute("profileRequest") UserUpdateRequest profileRequest,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            preparePage(model, authentication);
            return "user/mypage";
        }

        try {
            userService.updateMyPage(authentication, profileRequest);
            redirectAttributes.addFlashAttribute("profileSuccess", "회원정보가 저장되었습니다.");
            return "redirect:/mypage";
        } catch (IllegalArgumentException e) {
            bindingResult.reject("profileError", e.getMessage());
            preparePage(model, authentication);
            return "user/mypage";
        }
    }

    @PostMapping("/password")
    public String changePassword(
            @Valid @ModelAttribute("passwordRequest") PasswordChangeRequest passwordRequest,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            preparePage(model, authentication);
            return "user/mypage";
        }

        try {
            userService.changePassword(authentication, passwordRequest);
            redirectAttributes.addFlashAttribute("passwordSuccess", "비밀번호가 변경되었습니다.");
            return "redirect:/mypage#account-security";
        } catch (IllegalArgumentException e) {
            bindingResult.reject("passwordError", e.getMessage());
            preparePage(model, authentication);
            return "user/mypage";
        }
    }

    @PostMapping("/withdraw")
    public String withdraw(
            @Valid @ModelAttribute("withdrawRequest") WithdrawRequest withdrawRequest,
            BindingResult bindingResult,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            preparePage(model, authentication);
            return "user/mypage";
        }

        try {
            userService.withdraw(authentication, withdrawRequest);
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            return "redirect:/login?withdrawn=true";
        } catch (IllegalArgumentException e) {
            bindingResult.reject("withdrawError", e.getMessage());
            preparePage(model, authentication);
            return "user/mypage";
        }
    }

    /**
     * 마이페이지 렌더링에 필요한 모든 데이터를 모델에 담는 공통 메서드
     */
    private void preparePage(Model model, Authentication authentication) {
        MyPageResponse user = userService.getMyPage(authentication);
        model.addAttribute("user", user);

        // 관심 정책 목록 추가
        List<FavoriteDetailDto> favorites = favoriteService.getFavoritesWithDetails(user.getUserId());
        model.addAttribute("favorites", favorites);

        // 각종 요청 DTO 초기화
        if (!model.containsAttribute("profileRequest")) {
            model.addAttribute("profileRequest", UserUpdateRequest.from(user));
        }
        if (!model.containsAttribute("passwordRequest")) {
            model.addAttribute("passwordRequest", new PasswordChangeRequest());
        }
        if (!model.containsAttribute("withdrawRequest")) {
            model.addAttribute("withdrawRequest", new WithdrawRequest());
        }
    }
}