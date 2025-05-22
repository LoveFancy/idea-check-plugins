package com.example.demo;

import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;

public class DemoApp {
    public static void main(String[] args) {
        // 使用废弃的 Date 构造函数
        Date date = new Date(2024, 5, 22);  // 已废弃的构造函数

        // 使用废弃的 Vector 类
        Vector<String> vector = new Vector<>();
        vector.add("test");

        // 使用废弃的 Enumeration 接口
        Enumeration<String> enumeration = vector.elements();
        while (enumeration.hasMoreElements()) {
            System.out.println(enumeration.nextElement());
        }

        // 使用废弃的 Thread 方法
        Thread.currentThread().stop();  // 已废弃的方法
    }

    // 使用废弃的 @Deprecated 注解标记的方法
    @Deprecated
    public void oldMethod() {
        System.out.println("This is an old method");
    }

    // 使用废弃的 @Deprecated 注解标记的字段
    @Deprecated
    private String oldField = "This is an old field";
} 