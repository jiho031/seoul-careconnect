package com.seoulcareconnect.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {

    @GetMapping("/admin")
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }


    @GetMapping("/admin/policy-errors")
    public String policyErrors() {
        return "admin/policy-errors";
    }

    @GetMapping("/admin/duplicates")
    public String duplicates() {
        return "admin/duplicates";
    }

    @GetMapping("/admin/ai-review")
    public String aiReview() {
        return "admin/ai-review";
    }

    @GetMapping("/admin/reports")
    public String reports() {
        return "admin/reports";
    }

    @GetMapping("/admin/policies")
    public String policies() {
        return "admin/policies";
    }

    @GetMapping("/admin/policies/new")
    public String policyForm() {
        return "admin/policy-form";
    }

    @GetMapping("/admin/policies/{id}/edit")
    public String policyEdit() {
        return "admin/policy-form";
    }

    @GetMapping("/admin/users")
    public String users() {
        return "admin/users";
    }

    @GetMapping("/admin/super")
    public String superAdmin() {
        return "admin/super-admin";
    }

}