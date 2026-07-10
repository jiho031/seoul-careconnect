package com.seoulcareconnect.config.user;

import com.seoulcareconnect.dto.user.MyPageResponse;
import com.seoulcareconnect.dto.user.PasswordChangeRequest;
import com.seoulcareconnect.dto.user.UserUpdateRequest;
import com.seoulcareconnect.dto.user.WithdrawRequest;
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

@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {

    private final UserService userService;

    @GetMapping
    public String myPage(
            Authentication authentication,
            Model model
    ) {
        MyPageResponse user = userService.getMyPage(authentication);

        model.addAttribute("user", user);

        if (!model.containsAttribute("profileRequest")) {
            model.addAttribute(
                    "profileRequest",
                    UserUpdateRequest.from(user)
            );
        }

        if (!model.containsAttribute("passwordRequest")) {
            model.addAttribute(
                    "passwordRequest",
                    new PasswordChangeRequest()
            );
        }

        if (!model.containsAttribute("withdrawRequest")) {
            model.addAttribute(
                    "withdrawRequest",
                    new WithdrawRequest()
            );
        }

        return "user/mypage";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @Valid @ModelAttribute("profileRequest")
            UserUpdateRequest profileRequest,
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
            redirectAttributes.addFlashAttribute(
                    "profileSuccess",
                    "회원정보가 저장되었습니다."
            );
            return "redirect:/mypage";
        } catch (IllegalArgumentException e) {
            bindingResult.reject("profileError", e.getMessage());
            preparePage(model, authentication);
            return "user/mypage";
        }
    }

    @PostMapping("/password")
    public String changePassword(
            @Valid @ModelAttribute("passwordRequest")
            PasswordChangeRequest passwordRequest,
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
            redirectAttributes.addFlashAttribute(
                    "passwordSuccess",
                    "비밀번호가 변경되었습니다."
            );
            return "redirect:/mypage#account-security";
        } catch (IllegalArgumentException e) {
            bindingResult.reject("passwordError", e.getMessage());
            preparePage(model, authentication);
            return "user/mypage";
        }
    }

    @PostMapping("/withdraw")
    public String withdraw(
            @Valid @ModelAttribute("withdrawRequest")
            WithdrawRequest withdrawRequest,
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

            new SecurityContextLogoutHandler()
                    .logout(request, response, authentication);

            return "redirect:/login?withdrawn=true";
        } catch (IllegalArgumentException e) {
            bindingResult.reject("withdrawError", e.getMessage());
            preparePage(model, authentication);
            return "user/mypage";
        }
    }

    private void preparePage(
            Model model,
            Authentication authentication
    ) {
        MyPageResponse user = userService.getMyPage(authentication);
        model.addAttribute("user", user);

        if (!model.containsAttribute("profileRequest")) {
            model.addAttribute(
                    "profileRequest",
                    UserUpdateRequest.from(user)
            );
        }

        if (!model.containsAttribute("passwordRequest")) {
            model.addAttribute(
                    "passwordRequest",
                    new PasswordChangeRequest()
            );
        }

        if (!model.containsAttribute("withdrawRequest")) {
            model.addAttribute(
                    "withdrawRequest",
                    new WithdrawRequest()
            );
        }
    }
}
