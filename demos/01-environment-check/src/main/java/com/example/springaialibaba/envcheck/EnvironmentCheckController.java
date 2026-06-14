package com.example.springaialibaba.envcheck;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class EnvironmentCheckController {

    private final ChatClient chatClient;
    private final String applicationName;
    private final String apiKey;
    private final String model;

    public EnvironmentCheckController(ChatClient.Builder chatClientBuilder,
                                      @Value("${spring.application.name}") String applicationName,
                                      @Value("${spring.ai.dashscope.api-key:}") String apiKey,
                                      @Value("${spring.ai.dashscope.chat.options.model:qwen-plus}") String model) {
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一个 Spring AI Alibaba 环境检查助手。回答要简洁。")
                .build();
        this.applicationName = applicationName;
        this.apiKey = apiKey;
        this.model = model;
    }

    @GetMapping("/env/status")
    public EnvironmentStatus status() {
        return new EnvironmentStatus(
                applicationName,
                model,
                hasText(apiKey),
                maskApiKey(apiKey),
                "Spring AI Alibaba DashScope configuration loaded",
                Instant.now().toString()
        );
    }

    @GetMapping("/env/ping")
    public ModelPingResult ping(@RequestParam(defaultValue = "请只回答 OK，用于验证 Spring AI Alibaba 模型调用是否成功。") String message) {
        if (!hasText(apiKey)) {
            return new ModelPingResult(false, model, null, "DASHSCOPE_API_KEY is not configured");
        }

        try {
            String content = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();
            return new ModelPingResult(true, model, content, null);
        }
        catch (Exception ex) {
            return new ModelPingResult(false, model, null, ex.getClass().getSimpleName() + ": " + ex.getMessage());
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

    public record EnvironmentStatus(
            String applicationName,
            String model,
            boolean apiKeyConfigured,
            String apiKeyPreview,
            String message,
            String checkedAt
    ) {
    }

    public record ModelPingResult(
            boolean success,
            String model,
            String content,
            String error
    ) {
    }
}
