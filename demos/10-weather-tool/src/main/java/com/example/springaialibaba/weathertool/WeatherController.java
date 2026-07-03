package com.example.springaialibaba.weathertool;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 天气助手 Controller。
 *
 * 学习要点：
 * 1. 工具返回“结构化 record + 显式 error 字段”，而不是抛异常，模型更容易生成友好的话术。
 * 2. System Prompt 明确“先查工具再回答，不知道就说不知道”，避免模型编造。
 * 3. 通过 conversationId 也能加多轮记忆，本课先聚焦 Tool Calling，保留纯单轮。
 */
@RestController
public class WeatherController {

    private final ChatClient chatClient;
    private final String apiKey;

    public WeatherController(ChatClient.Builder builder,
                             WeatherTools tools,
                             @Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        this.chatClient = builder
                .defaultSystem("""
                        你是一名“出行天气助手”。规则：
                        1. 涉及具体城市天气 / 温度 / 出行建议时，必须先调用工具，禁止凭想象回答。
                        2. 工具返回 supported=false 时，礼貌告知用户目前支持的城市列表。
                        3. 最终回答使用中文，先给结论（是否适合出行），再给数据依据。
                        4. 只回答与天气 / 出行相关的问题，其它话题礼貌拒绝。
                        """)
                .defaultTools(tools)
                .build();
        this.apiKey = apiKey;
    }

    @PostMapping("/weather/ask")
    public Object ask(@RequestBody AskRequest req) {
        if (apiKey == null || apiKey.isBlank()) return Map.of("success", false, "error", "DASHSCOPE_API_KEY is not configured");
        if (req == null || req.question() == null || req.question().isBlank())
            return Map.of("success", false, "error", "question must not be blank");
        try {
            String answer = chatClient.prompt().user(req.question()).call().content();
            return Map.of("success", true, "question", req.question(), "answer", answer);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public record AskRequest(String question) {}
}
