package com.example.springaialibaba.chatmemory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 多轮对话 Controller。
 *
 * 教学要点：
 * 1. 请求里的 conversationId 决定这条消息属于哪个会话。
 * 2. 通过 advisor param BaseChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY 传入 conversationId。
 * 3. 取回消息数由 MessageWindowChatMemory 的窗口大小决定（构造时配置），不再通过 advisor param 传。
 * 4. /chat/history 与 /chat/reset 用于观察和清理会话。
 */
@RestController
public class ChatMemoryController {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final String apiKey;

    public ChatMemoryController(ChatClient memoryChatClient,
                                ChatMemory chatMemory,
                                @Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        this.chatClient = memoryChatClient;
        this.chatMemory = chatMemory;
        this.apiKey = apiKey;
    }

    @PostMapping("/chat/ask")
    public Object ask(@RequestBody AskRequest req) {
        if (!hasKey()) return Map.of("success", false, "error", "DASHSCOPE_API_KEY is not configured");
        if (req == null || isBlank(req.question())) return Map.of("success", false, "error", "question must not be blank");
        String cid = isBlank(req.conversationId()) ? "default" : req.conversationId();

        try {
            String answer = chatClient.prompt()
                    .user(req.question())
                    .advisors(a -> a.param("chat_memory_conversation_id", cid))
                    .call()
                    .content();
            return Map.of("success", true, "conversationId", cid,
                    "question", req.question(), "answer", answer);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** 查看某个会话保存的消息。Spring AI 1.1+ get() 只接受 conversationId。 */
    @GetMapping("/chat/history")
    public Object history(@RequestParam(defaultValue = "default") String conversationId) {
        List<Message> messages = chatMemory.get(conversationId);
        return Map.of("conversationId", conversationId, "count", messages.size(),
                "messages", messages.stream().map(m -> Map.of(
                        "type", m.getMessageType().name(),
                        "text", m.getText()
                )).toList());
    }

    @PostMapping("/chat/reset")
    public Object reset(@RequestParam(defaultValue = "default") String conversationId) {
        chatMemory.clear(conversationId);
        return Map.of("success", true, "conversationId", conversationId, "message", "cleared");
    }

    private boolean hasKey() { return apiKey != null && !apiKey.isBlank(); }
    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    public record AskRequest(String conversationId, String question) {}
}
