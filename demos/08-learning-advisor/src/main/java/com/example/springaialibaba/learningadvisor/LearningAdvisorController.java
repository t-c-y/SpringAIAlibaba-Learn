package com.example.springaialibaba.learningadvisor;

import com.example.springaialibaba.learningadvisor.LearnerProfileStore.LearnerProfile;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学习顾问 Controller（兼容 Spring AI 1.1.x）。
 *
 * 两层记忆：
 * - 短期：MessageChatMemoryAdvisor + MessageWindowChatMemory（替代旧版 InMemoryChatMemory）。
 * - 长期：LearnerProfileStore，每次把 profile 片段拼进 System Prompt。
 */
@Configuration
@RestController
public class LearningAdvisorController {

    private final ChatMemory chatMemory;
    private final ChatClient.Builder builder;
    private final LearnerProfileStore profileStore;
    private final String apiKey;

    public LearningAdvisorController(ChatClient.Builder builder,
                                     LearnerProfileStore profileStore,
                                     @Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        this.builder = builder;
        this.chatMemory = MessageWindowChatMemory.builder().build();
        this.profileStore = profileStore;
        this.apiKey = apiKey;
    }

    @Bean
    public ChatMemory learningAdvisorMemory() { return chatMemory; }

    private ChatClient buildClientFor(LearnerProfile profile) {
        String system = """
                你是一名"个人学习顾问"，服务对象已有的画像如下：

                %s

                请遵循以下规则回答：
                1. 使用中文，先给结论，再分点。
                2. 如果画像信息不足以给出建议，可先提出补充问题，但不要超过 2 个。
                3. 建议要落到"下一周可执行的动作"，不要抽象口号。
                4. 遇到与画像明显冲突的诉求（例如学生说想一周内成为架构师），先诚实提示不现实。
                """.formatted(profile.toSystemFragment());

        return builder
                .defaultSystem(system)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @PostMapping("/advisor/profile")
    public Object upsert(@RequestBody LearnerProfile profile) {
        if (profile == null || profile.userId() == null || profile.userId().isBlank()) {
            return Map.of("success", false, "error", "userId must not be blank");
        }
        LearnerProfile normalized = new LearnerProfile(
                profile.userId(),
                profile.name(),
                profile.targetRole(),
                profile.mastered() == null ? List.of() : profile.mastered(),
                profile.learning() == null ? List.of() : profile.learning(),
                profile.weeklyBudgetHours()
        );
        profileStore.update(normalized);
        return Map.of("success", true, "profile", normalized);
    }

    @GetMapping("/advisor/profile")
    public LearnerProfile getProfile(@RequestParam String userId) {
        return profileStore.getOrEmpty(userId);
    }

    @DeleteMapping("/advisor/profile")
    public Object deleteProfile(@RequestParam String userId) {
        profileStore.clear(userId);
        return Map.of("success", true);
    }

    @PostMapping("/advisor/ask")
    public Object ask(@RequestBody AskRequest req) {
        if (apiKey == null || apiKey.isBlank()) return Map.of("success", false, "error", "DASHSCOPE_API_KEY is not configured");
        if (req == null || req.userId() == null) return Map.of("success", false, "error", "userId must not be blank");
        if (req.question() == null || req.question().isBlank()) return Map.of("success", false, "error", "question must not be blank");

        LearnerProfile profile = profileStore.getOrEmpty(req.userId());
        ChatClient client = buildClientFor(profile);
        String cid = req.conversationId() == null ? req.userId() : req.conversationId();

        try {
            String answer = client.prompt()
                    .user(req.question())
                    // Spring AI 1.1 使用字符串 key "chat_memory_conversation_id" 指定会话 ID。
                    .advisors(a -> a.param("chat_memory_conversation_id", cid))
                    .call()
                    .content();
            return Map.of("success", true, "userId", req.userId(),
                    "conversationId", cid, "profileApplied", profile, "answer", answer);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public record AskRequest(String userId, String conversationId, String question) {}
}
