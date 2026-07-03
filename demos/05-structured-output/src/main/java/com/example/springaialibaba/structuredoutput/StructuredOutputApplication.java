package com.example.springaialibaba.structuredoutput;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 阶段五启动类：让模型输出可以被 Java 直接反序列化的结构化数据。 */
@SpringBootApplication
public class StructuredOutputApplication {
    public static void main(String[] args) {
        SpringApplication.run(StructuredOutputApplication.class, args);
    }
}
