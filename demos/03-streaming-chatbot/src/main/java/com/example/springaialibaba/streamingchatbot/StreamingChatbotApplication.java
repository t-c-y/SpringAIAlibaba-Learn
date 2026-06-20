package com.example.springaialibaba.streamingchatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 阶段三 Demo 启动类。
 *
 * 本阶段仍然是标准 Spring Boot 应用。
 * 和阶段二相比，核心变化是新增 SSE 流式接口，模型回答可以边生成边返回。
 */
@SpringBootApplication
public class StreamingChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamingChatbotApplication.class, args);
    }
}
