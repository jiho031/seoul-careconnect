package com.seoulcareconnect.controller;

import com.seoulcareconnect.service.policy.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final PolicyService policyService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute(
                "closingSoonPolicies",
                policyService.closingSoon(4)
        );

        model.addAttribute(
                "recentPolicies",
                policyService.recent(4)
        );

        model.addAttribute(
                "popularPolicies",
                policyService.popular(10)
        );

        return "index";
    }
}