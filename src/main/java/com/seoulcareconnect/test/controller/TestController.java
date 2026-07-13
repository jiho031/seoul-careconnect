package com.seoulcareconnect.test.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {

    @GetMapping("/reports")
    public String reportForm() {
        return "report/form";
    }

}
