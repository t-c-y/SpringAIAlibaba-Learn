package com.example.springaialibaba.chatbot;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * 最小 ChatBot Controller。
 *
 * 本阶段只做一件事：
 * 把用户问题通过 ChatClient 发给 DashScope / 百炼模型，并把模型回答返回给调用方。
 *
 * 暂时不引入 Memory、Tool Calling、RAG，是为了让你先把 ChatClient 的最小调用链路练熟。
 */
@RestController
public class ChatController {

    /**
     * ChatClient 是业务代码调用聊天模型的主要入口。
     */
    private final ChatClient chatClient;

    /**
     * 当前使用的模型名称，用于接口返回和排查问题。
     */
    private final String model;

    /**
     * DashScope / 百炼 API Key，只用于本地启动前检查，不会返回完整值。
     */
    private final String apiKey;

    public ChatController(ChatClient.Builder chatClientBuilder,
                          @Value("${spring.ai.dashscope.chat.options.model:qwen-plus}") String model,
                          @Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        你是一个企业级 Java 后端开发学习助手。
                        回答要求：
                        1. 使用中文。
                        2. 先给结论，再给关键原因。
                        3. 如果涉及代码，优先使用 Spring Boot 和 Spring AI Alibaba 示例。
                        4. 不确定时明确说明不确定，不要编造配置项或 API。
                        """)
                .build();
        this.model = model;
        this.apiKey = apiKey;
    }

    /**
     * ChatBot 状态接口。
     *
     * 这个接口不调用模型，只帮助你确认应用、模型名、API Key 配置状态。
     */
    @GetMapping("/chat/status")
    public ChatStatus status() {
        return new ChatStatus(
                model,
                hasText(apiKey),
                maskApiKey(apiKey),
                "ChatBot demo is ready",
                Instant.now().toString()
        );
    }

    /**
     * 同步问答接口。
     *
     * 示例请求：
     * POST /chat/ask
     * {"question":"Spring AI Alibaba 适合解决什么问题？"}
     */
    @PostMapping("/chat/ask")
    public AskResponse ask(@RequestBody AskRequest request) {
        if (!hasText(apiKey)) {
            return AskResponse.failed(model, "DASHSCOPE_API_KEY is not configured");
        }
        if (request == null || !hasText(request.question())) {
            return AskResponse.failed(model, "question must not be blank");
        }

        try {
            String answer = chatClient.prompt()
                    .user(request.question())
                    .call()
                    .content();
            return AskResponse.succeeded(model, request.question(), answer);
        }
        catch (Exception ex) {
            return AskResponse.failed(model, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String maskApiKey(String value) {
        if (!hasText(value)) {
            return "not configured";
        }
        if (value.length() <= 8) {
            return "********";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    public record ChatStatus(
            String model,
            boolean apiKeyConfigured,
            String apiKeyPreview,
            String message,
            String checkedAt
    ) {
    }

    public record AskRequest(String question) {
    }

    public record AskResponse(
            boolean success,
            String model,
            String question,
            String answer,
            String error
    ) {

        public static AskResponse succeeded(String model, String question, String answer) {
            return new AskResponse(true, model, question, answer, null);
        }

        public static AskResponse failed(String model, String error) {
            return new AskResponse(false, model, null, null, error);
        }
    }
}
