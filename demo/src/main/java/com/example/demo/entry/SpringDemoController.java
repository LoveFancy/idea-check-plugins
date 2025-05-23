package com.example.demo.entry;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpringDemoController {
    @RequestMapping("/hello")
    public String hello() {
        return "hello";
    }
} 