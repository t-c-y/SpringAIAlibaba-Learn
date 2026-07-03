package com.example.springaialibaba.promptassistant;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Prompt 学习助手 Controller。
 *
 * 本阶段核心练习：
 * 1. defaultSystem() 设定“角色 + 输出结构 + 边界”，让模型稳定输出学习卡片格式。
 * 2. 单独的 /ask/raw 接口不套 System Prompt，用于对比“无约束回答”的差异。
 * 3. /ask/lesson 接口接收 topic + level + audience，展示如何把业务字段拼进 user 消息。
 *
 * 暂时不引入结构化 JSON、Memory、Tool Calling，聚焦在“提示词工程”这一件事。
 */
@RestController
public class PromptAssistantController {

    /** 带 System Prompt 的“学习助手”客户端。 */
    private final ChatClient assistantClient;

    /** 不带 System Prompt 的“裸客户端”，只用于对比演示。 */
    private final ChatClient rawClient;

    private final String model;
    private final String apiKey;

    public PromptAssistantController(ChatClient.Builder builder,
                                     @Value("${spring.ai.dashscope.chat.options.model:qwen3.7-plus}") String model,
                                     @Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        this.assistantClient = builder
                .defaultSystem("""
                        # 角色
                        你是一名企业级 Java 后端 + Spring AI Alibaba 学习助手。
                        你的目标是把复杂概念讲给已经会 Spring Boot、但不熟悉大模型的中级开发者。

                        # 输出结构
                        每次回答按以下四段输出，每段之间空一行：
                        1. 【一句话结论】：不超过 40 字。
                        2. 【三点要点】：使用 1./ 2./ 3. 编号，每点不超过 60 字。
                        3. 【最小代码示例】：Java 或 YAML，必须能直接放进 Spring Boot 项目。
                        4. 【常见坑】：一句话即可，明确指出容易踩的点。

                        # 边界
                        - 不确定的 API、配置项一律说“不确定”，不要编造。
                        - 涉及生产建议时，必须提示“需结合具体业务评估”。
                        - 不回答与 Java / Spring / AI Agent 无关的问题，礼貌拒绝并说明原因。
                        """)
                .build();

        // rawClient 不设 defaultSystem，模型会用自己的默认语气回答，便于对比。
        this.rawClient = builder.build();

        this.model = model;
        this.apiKey = apiKey;
    }

    @GetMapping("/prompt/status")
    public PromptStatus status() {
        return new PromptStatus(model, hasText(apiKey), maskApiKey(apiKey),
                "prompt-engineering", "Prompt assistant demo is ready", Instant.now().toString());
    }

    /**
     * 使用带 System Prompt 的学习助手回答，能稳定得到“四段式”结构。
     */
    @PostMapping("/prompt/ask")
    public AskResponse ask(@RequestBody AskRequest request) {
        if (!hasText(apiKey)) {
            return AskResponse.failed(model, "DASHSCOPE_API_KEY is not configured");
        }
        if (request == null || !hasText(request.question())) {
            return AskResponse.failed(model, "question must not be blank");
        }
        try {
            String answer = assistantClient.prompt().user(request.question()).call().content();
            return AskResponse.succeeded(model, "assistant-system-prompt", request.question(), answer);
        } catch (Exception ex) {
            return AskResponse.failed(model, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    /**
     * 不带 System Prompt 的“裸调用”。对比后你会明显看到回答风格更松散。
     */
    @PostMapping("/prompt/ask/raw")
    public AskResponse askRaw(@RequestBody AskRequest request) {
        if (!hasText(apiKey)) {
            return AskResponse.failed(model, "DASHSCOPE_API_KEY is not configured");
        }
        if (request == null || !hasText(request.question())) {
            return AskResponse.failed(model, "question must not be blank");
        }
        try {
            String answer = rawClient.prompt().user(request.question()).call().content();
            return AskResponse.succeeded(model, "raw-no-system-prompt", request.question(), answer);
        } catch (Exception ex) {
            return AskResponse.failed(model, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    /**
     * 面向“课程生成”场景：把 topic / level / audience 拼进 user 消息。
     * 这里演示 Prompt 工程的常见套路——业务字段进入 user 消息，风格约束留在 System Prompt。
     */
    @PostMapping("/prompt/ask/lesson")
    public AskResponse askLesson(@RequestBody LessonRequest request) {
        if (!hasText(apiKey)) {
            return AskResponse.failed(model, "DASHSCOPE_API_KEY is not configured");
        }
        if (request == null || !hasText(request.topic())) {
            return AskResponse.failed(model, "topic must not be blank");
        }
        String level = hasText(request.level()) ? request.level() : "入门";
        String audience = hasText(request.audience()) ? request.audience() : "Java 后端开发者";
        String userMessage = """
                请为下面这堂课生成教学内容。

                【主题】%s
                【难度】%s
                【面向听众】%s

                严格按 System Prompt 中的四段结构输出。
                """.formatted(request.topic(), level, audience);
        try {
            String answer = assistantClient.prompt().user(userMessage).call().content();
            return AskResponse.succeeded(model, "assistant-lesson-template", userMessage, answer);
        } catch (Exception ex) {
            return AskResponse.failed(model, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private boolean hasText(String v) { return v != null && !v.isBlank(); }

    private String maskApiKey(String v) {
        if (!hasText(v)) return "not configured";
        if (v.length() <= 8) return "********";
        return v.substring(0, 4) + "****" + v.substring(v.length() - 4);
    }

    public record PromptStatus(String model, boolean apiKeyConfigured, String apiKeyPreview,
                               String mode, String message, String checkedAt) {}
    public record AskRequest(String question) {}
    public record LessonRequest(String topic, String level, String audience) {}
    public record AskResponse(boolean success, String model, String mode, String question, String answer, String error) {
        public static AskResponse succeeded(String m, String mode, String q, String a) {
            return new AskResponse(true, m, mode, q, a, null);
        }
        public static AskResponse failed(String m, String err) {
            return new AskResponse(false, m, null, null, null, err);
        }
    }
}
