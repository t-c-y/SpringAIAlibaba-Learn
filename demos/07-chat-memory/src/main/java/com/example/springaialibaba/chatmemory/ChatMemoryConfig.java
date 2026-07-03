package com.example.springaialibaba.chatmemory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 会话记忆配置。
 *
 * Spring AI 1.1+ 使用 MessageWindowChatMemory（替代旧版 InMemoryChatMemory）：
 * - 内部基于 InMemoryChatMemoryRepository。
 * - 按“最近 N 条消息”保留窗口，默认 20 条。
 *
 * 生产环境应把 ChatMemory 换为基于 Redis / DB 的实现。
 */
@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemory chatMemory() {
        // 保留最近 20 条消息（默认值），可通过 .maxMessages(...) 调整。
        return MessageWindowChatMemory.builder().build();
    }

    @Bean
    public ChatClient memoryChatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultSystem("""
                        你是一名企业级 Java 后端 + Spring AI Alibaba 学习助手。
                        请充分利用历史对话中提供的信息，例如用户的名字、目标、已经掌握的知识点。
                        回答保持中文、简洁、可执行。
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
