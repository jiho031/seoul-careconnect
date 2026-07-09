package com.seoulcareconnect.test.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {

    @GetMapping("/policies")
    public String policyList() {
        return "policy/list";
    }
    @GetMapping("/policies/detail")
    public String policyDetail() {
        return "policy/detail";
    }
    @GetMapping("/reports")
    public String reportForm() {
        return "report/form";
    }
}
