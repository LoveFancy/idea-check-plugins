package com.example.demo;

public class DemoApplication {
    public static void main(String[] args) {
        DemoService service = new DemoService();
        
        // 使用废弃的方法
        service.oldMethod();
        service.anotherOldMethod();
        
        // 使用新的方法
        service.newMethod();
        service.anotherNewMethod();
    }
} 