package com.example.springaialibaba.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 阶段二 Demo 启动类。
 *
 * 和阶段一一样，Spring AI Alibaba 应用仍然是标准 Spring Boot 应用。
 * 区别在于：阶段一只验证环境，阶段二开始提供真正面向用户的 ChatBot 接口。
 */
@SpringBootApplication
public class ChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatbotApplication.class, args);
    }
}
