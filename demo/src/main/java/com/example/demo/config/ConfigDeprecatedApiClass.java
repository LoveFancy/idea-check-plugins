package com.example.demo.config;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ConfigDeprecatedApiClass {

    @RequestMapping("/configDeprecated")
    public void configDeprgecatedMethod() {
        System.out.println("This method is deprecated by config");
        testB();
    }

    private void testB () {
        System.out.println("This method is deprecated by config");
        testC();
    }

    private void testC  () {
        System.out.println("This method is deprecated by config");
    }

    private void testD() {
        System.out.println("This method is deprecated by config");
        testC();
    }
}