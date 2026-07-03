package com.example.springaialibaba.prompttemplate;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Prompt 模板 Controller。
 *
 * 学习要点：
 * 1. PromptTemplate.create(Map): 把变量插值到模板文本，产出可以直接使用的字符串。
 * 2. SystemPromptTemplate.createMessage(Map): 生成一条 SystemMessage，用于组装多消息 Prompt。
 * 3. 从 classpath resources 加载 .st 模板文件，让 Prompt 脱离 Java 代码维护。
 */
@RestController
public class PromptTemplateController {

    private final ChatClient chatClient;
    private final String apiKey;

    /** 从 classpath 加载的 System Prompt 模板文件。 */
    @Value("classpath:prompts/lesson-assistant-system.st")
    private Resource systemPromptResource;

    /** 用户消息模板文件。 */
    @Value("classpath:prompts/lesson-user.st")
    private Resource userPromptResource;

    public PromptTemplateController(ChatClient.Builder builder,
                                    @Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        this.chatClient = builder.build();
        this.apiKey = apiKey;
    }

    /**
     * 用文件模板生成课程内容。System Prompt 和 User Prompt 都来自 resources/prompts/*.st。
     */
    @PostMapping("/tmpl/lesson")
    public Object lesson(@RequestBody LessonRequest req) throws IOException {
        if (!hasKey()) return Map.of("success", false, "error", "DASHSCOPE_API_KEY is not configured");
        if (req == null || isBlank(req.topic())) return Map.of("success", false, "error", "topic must not be blank");

        String systemTemplate = read(systemPromptResource);
        String userTemplate = read(userPromptResource);

        Message system = new SystemPromptTemplate(systemTemplate)
                .createMessage(Map.of(
                        "role", nvl(req.role(), "Spring AI Alibaba 学习助手"),
                        "style", nvl(req.style(), "严谨、简洁、代码优先")
                ));

        String userText = new PromptTemplate(userTemplate)
                .render(Map.of(
                        "topic", req.topic(),
                        "level", nvl(req.level(), "入门"),
                        "audience", nvl(req.audience(), "Java 后端开发者"),
                        "duration", nvl(req.duration(), "45 分钟")
                ));

        // 组装多消息 Prompt，然后调用模型。
        Prompt prompt = new Prompt(List.of(system, new org.springframework.ai.chat.messages.UserMessage(userText)));
        try {
            String answer = chatClient.prompt(prompt).call().content();
            return Map.of("success", true, "userMessage", userText, "answer", answer);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** 演示纯字符串模板：只用 PromptTemplate.render() 拼提示词。 */
    @PostMapping("/tmpl/simple")
    public Object simple(@RequestBody LessonRequest req) {
        if (!hasKey()) return Map.of("success", false, "error", "DASHSCOPE_API_KEY is not configured");
        String template = "请用一句话解释 {topic} 对 {audience} 的价值，控制在 40 字以内。";
        String user = new PromptTemplate(template).render(Map.of(
                "topic", nvl(req.topic(), "Spring AI Alibaba"),
                "audience", nvl(req.audience(), "Java 后端开发者")
        ));
        String answer = chatClient.prompt().user(user).call().content();
        return Map.of("success", true, "rendered", user, "answer", answer);
    }

    private String read(Resource res) throws IOException {
        try (var in = res.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private boolean hasKey() { return apiKey != null && !apiKey.isBlank(); }
    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private String nvl(String v, String d) { return isBlank(v) ? d : v; }

    public record LessonRequest(String topic, String level, String audience,
                                String duration, String role, String style) {}
}
