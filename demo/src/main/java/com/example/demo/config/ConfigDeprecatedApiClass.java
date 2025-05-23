package com.example.demo.config;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ConfigDeprecatedApiClass {
    @RequestMapping("/configDeprecated")
    public void configDeprecatedMethod() {
        System.out.println("This method is deprecated by config");
    }
} 