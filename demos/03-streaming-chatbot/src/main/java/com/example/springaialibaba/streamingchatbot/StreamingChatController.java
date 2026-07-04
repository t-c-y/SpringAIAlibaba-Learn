package com.example.springaialibaba.streamingchatbot;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Instant;

/**
 * 流式 ChatBot Controller。
 *
 * 本阶段在阶段二同步问答的基础上，只新增一个核心能力：
 * 使用 ChatClient 的 stream() 方法，将模型生成过程通过 SSE 持续返回给客户端。
 *
 * 暂时不引入 Memory、Tool Calling、RAG，是为了让你先把“流式输出”这个能力单独练熟。
 */
@RestController
public class StreamingChatController {

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

    public StreamingChatController(ChatClient.Builder chatClientBuilder,
                                   @Value("${spring.ai.dashscope.chat.options.model:qwen3.7-max}") String model,
                                   @Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        你是一个企业级 Java 后端开发学习助手。
                        回答要求：
                        1. 使用中文。
                        2. 先给结论，再分点说明。
                        3. 尽量用 Spring Boot 和 Spring AI Alibaba 的语境解释。
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
                "streaming-sse",
                "Streaming ChatBot demo is ready",
                Instant.now().toString()
        );
    }

    /**
     * 同步问答接口。
     *
     * 这个接口用于和 /chat/stream 做对比：
     * - /chat/ask 会等待模型完整生成后一次性返回 JSON。
     * - /chat/stream 会边生成边通过 SSE 返回文本片段。
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

    /**
     * 流式问答接口。
     *
     * produces = text/event-stream 表示这个接口返回 SSE 流。
     * curl 测试时建议加 -N，避免 curl 自身缓冲导致看不到逐步输出效果。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestBody AskRequest request) {
        if (!hasText(apiKey)) {
            return Flux.just("DASHSCOPE_API_KEY is not configured");
        }
        if (request == null || !hasText(request.question())) {
            return Flux.just("question must not be blank");
        }

        return chatClient.prompt()
                .user(request.question())
                .stream()
                .content()
                .onErrorResume(ex -> Flux.just("\n[ERROR] " + ex.getClass().getSimpleName() + ": " + ex.getMessage()));
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
            String mode,
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
