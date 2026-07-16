package com.seoulcareconnect.controller.user;

import com.seoulcareconnect.dto.user.SignupRequest;
import com.seoulcareconnect.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/signup")
    public String signupPage(Model model) {

        if (!model.containsAttribute("signupRequest")) {
            model.addAttribute(
                    "signupRequest",
                    new SignupRequest()
            );
        }

        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(
            @Valid
            @ModelAttribute("signupRequest")
            SignupRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }

        try {
            userService.signup(request);
            return "redirect:/login?signup=true";

        } catch (IllegalArgumentException e) {
            bindingResult.reject(
                    "signupError",
                    e.getMessage()
            );

            return "auth/signup";
        }
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }
}