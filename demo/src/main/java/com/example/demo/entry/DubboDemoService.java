package com.example.demo.entry;

import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class DubboDemoService {
    public void dubboMethod() {
        System.out.println("Dubbo service method");
    }

    public void dubboMethod2() {
        System.out.println("Dubbo service method 2");
    }
} 