package com.example.springaialibaba.platform;

import org.springframework.ai.chat.client.ChatClient;

/**
 * Agent 抽象。每个 Agent 有：
 * - id：全局唯一，供路由使用
 * - description：用于展示与检索
 * - handle：真正处理业务的入口
 */
public interface Agent {
    String id();
    String description();
    AgentResponse handle(AgentRequest req);

    record AgentRequest(String userId, String question) {}
    record AgentResponse(String agentId, String answer, long latencyMs) {}
}
