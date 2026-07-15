package com.seoulcareconnect.controller.user;

import com.seoulcareconnect.entity.user.Policy;
import com.seoulcareconnect.repository.user.UserRepository;
import com.seoulcareconnect.service.user.PolicyService; // 추가
import com.seoulcareconnect.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PolicyController {

    private final UserRepository userRepository;
    private final PolicyService policyService; // 추가

    @GetMapping("/policy/list")
    public String policyList(Authentication authentication,
                             Model model) {

        if(authentication != null){
            User user = userRepository
                    .findByEmail(authentication.getName())
                    .orElse(null);
            model.addAttribute("user", user);
        }

        // 정책 목록을 조회하여 모델에 추가
        List<Policy> policyList = policyService.getAllPolicies();
        model.addAttribute("policyList", policyList);

        return "policy/list";
    }
}