package com.seoulcareconnect.controller;

import com.seoulcareconnect.service.policy.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ApplicationGuideController {

    private final PolicyService policyService;

    @GetMapping("/guides")
    public String applicationGuide(Model model) {
        model.addAttribute("guidePolicies", policyService.recent(3));
        return "guide/index";
    }
}
