package com.example.springaialibaba.envcheck;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * 环境检查 Controller。
 *
 * 这个类是阶段一 Demo 的核心代码，主要用于学习：
 * 1. 如何从 Spring 配置中读取 Spring AI Alibaba 相关参数。
 * 2. 如何注入 ChatClient.Builder。
 * 3. 如何构建 ChatClient。
 * 4. 如何提供一个“不调用模型”的环境检查接口。
 * 5. 如何提供一个“调用模型”的最小验证接口。
 *
 * 注意：
 * 这个 Demo 不是完整 ChatBot，只是环境检查 Demo。
 * 这里的代码故意保持简单，方便你先理解 Spring AI Alibaba 的最小接入流程。
 */
@RestController
public class EnvironmentCheckController {

    /**
     * ChatClient 是 Spring AI 中面向聊天模型的高层客户端。
     *
     * 你可以把它理解成：
     * Java 后端调用大模型的统一入口。
     *
     * 本 Demo 中它负责向 DashScope / 百炼模型发送一条最简单的消息，
     * 用来验证模型调用链路是否正常。
     */
    private final ChatClient chatClient;

    /**
     * 当前 Spring Boot 应用名称。
     *
     * 来源：application.yml 中的 spring.application.name。
     * 这里只是为了在 /env/status 接口中展示当前应用配置是否读取成功。
     */
    private final String applicationName;

    /**
     * DashScope / 百炼 API Key。
     *
     * 来源：application.yml 中的 spring.ai.dashscope.api-key。
     * 通常这个值又来自环境变量 DASHSCOPE_API_KEY。
     *
     * 安全注意：
     * 真实项目中不要把完整 API Key 返回给前端、写入日志或提交到代码仓库。
     */
    private final String apiKey;

    /**
     * 当前使用的聊天模型名称。
     *
     * 来源：application.yml 中的 spring.ai.dashscope.chat.options.model。
     * 默认值通常可以使用 qwen-plus，也可以通过环境变量 DASHSCOPE_CHAT_MODEL 覆盖。
     */
    private final String model;

    /**
     * 构造方法注入。
     *
     * Spring 推荐使用构造方法注入，优点是：
     * 1. 依赖关系清晰。
     * 2. 字段可以声明为 final，避免对象创建后被修改。
     * 3. 更方便测试。
     *
     * 参数说明：
     *
     * @param chatClientBuilder Spring AI 自动配置好的 ChatClient.Builder。
     *                          只要 pom.xml 引入了 spring-ai-alibaba-starter-dashscope，
     *                          并且配置了 spring.ai.dashscope 相关参数，Spring 容器就会提供它。
     * @param applicationName   从 spring.application.name 读取的应用名。
     * @param apiKey            从 spring.ai.dashscope.api-key 读取的 API Key。
     * @param model             从 spring.ai.dashscope.chat.options.model 读取的模型名。
     */
    public EnvironmentCheckController(ChatClient.Builder chatClientBuilder,
                                      @Value("${spring.application.name}") String applicationName,
                                      @Value("${spring.ai.dashscope.api-key:}") String apiKey,
                                      @Value("${spring.ai.dashscope.chat.options.model:qwen-plus}") String model) {
        this.chatClient = chatClientBuilder
                // defaultSystem 用于设置默认系统提示词。
                // System Prompt 通常用于定义模型身份、行为边界和回答风格。
                // 这里设置为“环境检查助手”，只是为了让模型回答更简洁。
                .defaultSystem("你是一个 Spring AI Alibaba 环境检查助手。回答要简洁。")
                .build();
        this.applicationName = applicationName;
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * 环境状态检查接口。
     *
     * 请求地址：GET /env/status
     *
     * 这个接口不会调用大模型，只检查本地应用配置是否已经被 Spring 正确读取。
     *
     * 适合用来排查：
     * 1. 应用是否正常启动。
     * 2. 应用名是否读取成功。
     * 3. 模型名是否读取成功。
     * 4. API Key 是否已经配置。
     *
     * @return 当前环境配置状态。
     */
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

    /**
     * 模型调用验证接口。
     *
     * 请求地址：GET /env/ping
     * 示例：
     *   /env/ping
     *   /env/ping?message=请用一句话介绍 Spring AI Alibaba
     *
     * 这个接口会真正调用一次大模型，用于验证：
     * 1. API Key 是否有效。
     * 2. 模型名是否正确。
     * 3. 当前网络是否可以访问 DashScope / 百炼服务。
     * 4. Spring AI Alibaba 的 ChatClient 调用链路是否打通。
     *
     * @param message 用户传入的测试消息。如果不传，则使用默认消息。
     * @return 模型调用结果。成功时 content 有值；失败时 error 有值。
     */
    @GetMapping("/env/ping")
    public ModelPingResult ping(@RequestParam(defaultValue = "请只回答 OK，用于验证 Spring AI Alibaba 模型调用是否成功。") String message) {
        // 先在本地检查 API Key 是否为空。
        // 这样可以在调用模型之前快速给出明确错误，避免用户只看到底层 SDK 的异常信息。
        if (!hasText(apiKey)) {
            return new ModelPingResult(false, model, null, "DASHSCOPE_API_KEY is not configured");
        }

        try {
            String content = chatClient.prompt()
                    // user(...) 表示用户消息，也就是最终会发送给大模型的问题。
                    .user(message)
                    // call() 表示执行一次同步模型调用。
                    // 同步调用会等待模型生成完整结果后再返回。
                    .call()
                    // content() 表示只取模型回答中的文本内容。
                    .content();
            return new ModelPingResult(true, model, content, null);
        }
        catch (Exception ex) {
            // 阶段一先直接返回异常类型和异常消息，方便学习和排查。
            // 生产环境中通常不建议把完整异常信息直接返回给前端，
            // 而是记录日志，并返回更友好的错误码和提示。
            return new ModelPingResult(false, model, null, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    /**
     * 判断字符串是否有有效内容。
     *
     * 这里不用 Spring 的 StringUtils.hasText，是为了让 Demo 少一点额外工具类依赖，
     * 也方便你直接理解判断逻辑。
     *
     * @param value 待检查字符串。
     * @return true 表示字符串不是 null 且去掉空白后仍有内容。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 对 API Key 做脱敏展示。
     *
     * 为什么要脱敏：
     * 1. API Key 属于敏感凭证。
     * 2. 泄露后可能导致模型服务被他人调用，产生费用或安全风险。
     * 3. 环境检查接口只需要确认“有没有配置”，不应该暴露完整 Key。
     *
     * @param value 原始 API Key。
     * @return 脱敏后的字符串。
     */
    private String maskApiKey(String value) {
        if (!hasText(value)) {
            return "not configured";
        }
        if (value.length() <= 8) {
            return "********";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    /**
     * /env/status 接口的返回对象。
     *
     * Java record 是一种适合表达“只承载数据”的不可变对象的语法。
     * Spring Boot 会自动把 record 序列化成 JSON 返回给调用方。
     *
     * 字段说明：
     *
     * @param applicationName  当前应用名。
     * @param model            当前模型名。
     * @param apiKeyConfigured 是否已经配置 API Key。
     * @param apiKeyPreview    脱敏后的 API Key 预览。
     * @param message          环境检查说明。
     * @param checkedAt        检查时间。
     */
    public record EnvironmentStatus(
            String applicationName,
            String model,
            boolean apiKeyConfigured,
            String apiKeyPreview,
            String message,
            String checkedAt
    ) {
    }

    /**
     * /env/ping 接口的返回对象。
     *
     * 字段说明：
     *
     * @param success 是否调用成功。
     * @param model   当前模型名。
     * @param content 成功时的大模型回答内容。
     * @param error   失败时的错误信息。
     */
    public record ModelPingResult(
            boolean success,
            String model,
            String content,
            String error
    ) {
    }
}
