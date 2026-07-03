package com.example.springaialibaba.calculatortool;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Tool Calling Controller。
 *
 * 学习要点：
 * 1. 通过 .tools(new CalculatorTools()) 或 .defaultTools(...) 注册工具。
 * 2. 模型收到用户消息后，会自行判断是否要调用工具、调用哪一个、传什么参数。
 * 3. Spring AI 会自动把工具调用结果回填给模型，再让模型输出最终答案（一轮或多轮）。
 */
@RestController
public class CalculatorController {

    private final ChatClient chatClient;
    private final String apiKey;

    public CalculatorController(ChatClient.Builder builder,
                                CalculatorTools tools,
                                @Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        this.chatClient = builder
                .defaultSystem("""
                        你是一名精确的“计算助手”，遇到任何计算问题必须调用工具完成，不要口算。
                        规则：
                        1. 如果问题包含四则运算，务必逐步调用 add / subtract / multiply / divide。
                        2. 一次只能调用一个工具，一步一步来。
                        3. 最后用中文给出结论，格式：结果：<数值>。
                        """)
                .defaultTools(tools)
                .build();
        this.apiKey = apiKey;
    }

    @PostMapping("/calc/ask")
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
