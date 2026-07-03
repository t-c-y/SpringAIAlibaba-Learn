package com.example.springaialibaba.platform;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 三个内置 Agent：技术学习、订单客服、代码助手。真实项目里应当由配置中心 + Bean 装配注入。
 */
@Component
class LearningAgent implements Agent {
    private final ChatClient chat;
    LearningAgent(ChatClient.Builder b) {
        this.chat = b.defaultSystem("你是企业 Java + Spring AI Alibaba 学习助手，用中文简洁作答。").build();
    }
    public String id() { return "learning"; }
    public String description() { return "回答与 Spring AI Alibaba / Spring Boot / Java 后端相关的技术问题"; }
    public AgentResponse handle(AgentRequest r) {
        long t = System.nanoTime();
        String a = chat.prompt().user(r.question()).call().content();
        return new AgentResponse(id(), a, (System.nanoTime() - t) / 1_000_000L);
    }
}

@Component
class CustomerServiceAgent implements Agent {
    private final ChatClient chat;
    CustomerServiceAgent(ChatClient.Builder b) {
        this.chat = b.defaultSystem("你是电商客服助手。涉及订单/退款相关问题请给出流程性回答；避免承诺已经完成写操作。").build();
    }
    public String id() { return "customer-service"; }
    public String description() { return "处理订单、物流、退款、售后等电商类问题"; }
    public AgentResponse handle(AgentRequest r) {
        long t = System.nanoTime();
        String a = chat.prompt().user("当前登录用户：" + r.userId() + "\n问题：" + r.question()).call().content();
        return new AgentResponse(id(), a, (System.nanoTime() - t) / 1_000_000L);
    }
}

@Component
class CodingAgent implements Agent {
    private final ChatClient chat;
    CodingAgent(ChatClient.Builder b) {
        this.chat = b.defaultSystem("你是资深 Java 后端代码助手，回答需给出可编译的最小示例。").build();
    }
    public String id() { return "coding"; }
    public String description() { return "写 Java / Spring 代码片段、排查栈异常、重构建议"; }
    public AgentResponse handle(AgentRequest r) {
        long t = System.nanoTime();
        String a = chat.prompt().user(r.question()).call().content();
        return new AgentResponse(id(), a, (System.nanoTime() - t) / 1_000_000L);
    }
}
