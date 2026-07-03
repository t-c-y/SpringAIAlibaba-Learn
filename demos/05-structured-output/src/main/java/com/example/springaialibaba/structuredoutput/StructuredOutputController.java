package com.example.springaialibaba.structuredoutput;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 结构化输出 Controller。
 *
 * 教学要点：
 * 1. 用 .entity(Class) 让模型返回可直接反序列化的对象。
 * 2. 用 .entity(new ParameterizedTypeReference&lt;List&lt;T&gt;&gt;() {}) 返回集合。
 * 3. 对比“纯文本 -> 手动 parse”和“ChatClient.entity() 自动 parse”的差异。
 *
 * Spring AI 内部会自动把目标类型的 JSON Schema 拼接进 Prompt，并调用 BeanOutputConverter
 * 把模型返回的文本解析回 Java 对象。使用时你只需要传目标类型即可。
 */
@RestController
public class StructuredOutputController {

    private final ChatClient chatClient;
    private final String apiKey;

    public StructuredOutputController(ChatClient.Builder builder,
                                      @Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        this.chatClient = builder
                .defaultSystem("你是一个严谨的 IT 内容生成助手，回答必须严格符合调用方要求的 JSON 结构，不要输出多余字段或解释文字。")
                .build();
        this.apiKey = apiKey;
    }

    /** 单个对象：让模型返回一份“课程大纲”。 */
    @PostMapping("/struct/lesson")
    public Object lesson(@RequestBody TopicRequest req) {
        if (!hasKey()) return err("DASHSCOPE_API_KEY is not configured");
        if (req == null || isBlank(req.topic())) return err("topic must not be blank");
        try {
            LessonOutline outline = chatClient.prompt()
                    .user("请为主题《" + req.topic() + "》生成一份课程大纲。")
                    .call()
                    .entity(LessonOutline.class);
            return outline;
        } catch (Exception e) {
            return err(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** 列表：让模型返回一组“学习检查点”。 */
    @PostMapping("/struct/checkpoints")
    public Object checkpoints(@RequestBody TopicRequest req) {
        if (!hasKey()) return err("DASHSCOPE_API_KEY is not configured");
        if (req == null || isBlank(req.topic())) return err("topic must not be blank");
        try {
            List<Checkpoint> list = chatClient.prompt()
                    .user("请为《" + req.topic() + "》生成 5 条学习检查点，每条须是能明确判断的动作。")
                    .call()
                    .entity(new org.springframework.core.ParameterizedTypeReference<List<Checkpoint>>() {});
            return list;
        } catch (Exception e) {
            return err(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** 反例：不用 .entity()，直接拿 String，让你直观感受“手动 parse”的痛点。 */
    @PostMapping("/struct/raw")
    public Object raw(@RequestBody TopicRequest req) {
        if (!hasKey()) return err("DASHSCOPE_API_KEY is not configured");
        try {
            String text = chatClient.prompt()
                    .user("请为《" + req.topic() + "》返回一份 JSON 格式的课程大纲。")
                    .call()
                    .content();
            return java.util.Map.of("rawText", text, "hint",
                    "生产代码中不要用这种方式：模型可能返回 ```json 包裹、多余解释、字段名不一致。用 .entity() 就能避免。");
        } catch (Exception e) {
            return err(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private boolean hasKey() { return apiKey != null && !apiKey.isBlank(); }
    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private Object err(String msg) { return java.util.Map.of("success", false, "error", msg); }

    public record TopicRequest(String topic) {}
    public record LessonOutline(String title, String summary, List<String> sections, String difficulty) {}
    public record Checkpoint(String id, String description, String verifyMethod) {}
}
