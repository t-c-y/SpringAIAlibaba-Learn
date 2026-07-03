package com.example.springaialibaba.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 阶段十九：企业级 Agent 平台原型。
 *
 * 覆盖点：
 * - 多 Agent 注册与路由（AgentRegistry + AgentRouter）
 * - 模型网关抽象（ModelGateway，可切主备模型）
 * - 请求级审计与限流（RequestGuard）
 * - 用户身份贯穿（ThreadLocal 版 UserContext）
 *
 * 目标是形成一份可扩展的“骨架”，让你在此基础上继续加评测、观测、安全、成本控制。
 */
@SpringBootApplication
public class AgentPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentPlatformApplication.class, args);
    }
}
