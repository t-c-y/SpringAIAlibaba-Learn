package com.example.springaialibaba.chatmemory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 阶段七启动类：让 ChatBot 拥有多轮记忆，理解 conversationId 与会话隔离。 */
@SpringBootApplication
public class ChatMemoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatMemoryApplication.class, args);
    }
}
