package com.seoulcareconnect.test.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {

    @GetMapping({"/"})
    public String index() {
        return "index";
    }

    @GetMapping("/policies")
    public String policyList() {
        return "policy/list";
    }
    @GetMapping("/policies/detail")
    public String policyDetail() {
        return "policy/detail";
    }
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }
    @GetMapping("/signup")
    public String signup() {
        return "auth/signup";
    }
    @GetMapping("/reports")
    public String reportForm() {
        return "report/form";
    }
}
