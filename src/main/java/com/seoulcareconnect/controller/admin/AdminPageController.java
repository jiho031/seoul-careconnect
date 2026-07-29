package com.seoulcareconnect.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {

    @GetMapping("/admin")
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }

//    @GetMapping("/admin/policy-errors")
//    public String policyErrors() {
//        return "admin/policy-errors";
//    }

    @GetMapping("/admin/duplicates")
    public String duplicates() {
        return "admin/duplicates";
    }

//    @GetMapping("/admin/policies/new")
//    public String policyForm() {
//        return "admin/policy-form";
//    }

//    @GetMapping("/admin/policies/{id}/edit")
//    public String policyEdit() {
//        return "admin/policy-form";
//    }

//    @GetMapping("/admin/users")
//    public String users() {
//        return "admin/users";
//    }


}
