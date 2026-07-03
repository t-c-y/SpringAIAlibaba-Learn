package com.example.springaialibaba.orderagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 订单查询 Agent Controller。
 *
 * 教学要点：
 * 1. 把 X-User-Id 通过 UserContext.set 注入 ThreadLocal，工具方法从这里取，绝不信任 LLM。
 * 2. System Prompt 明确“不要在参数中写别的 userId”，但真正的权限守卫在 OrderTools 里。
 * 3. finally 里 clear，避免 ThreadLocal 泄漏到线程池的下一个请求。
 */
@RestController
public class OrderAgentController {

    private final ChatClient chatClient;
    private final String apiKey;

    public OrderAgentController(ChatClient.Builder builder,
                                OrderTools tools,
                                @Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        this.chatClient = builder
                .defaultSystem("""
                        你是一名“电商订单助手”，服务当前登录用户。规则：
                        1. 涉及订单信息，必须调用工具，禁止编造。
                        2. 不需要向工具传 userId，工具会自行从会话上下文识别登录用户。
                        3. 如果工具返回 allowed=false，请如实告知用户"无权限或订单不存在"，不要暴露内部原因。
                        4. 只回答订单相关问题，其它话题礼貌拒绝。
                        """)
                .defaultTools(tools)
                .build();
        this.apiKey = apiKey;
    }

    @PostMapping("/order/ask")
    public Object ask(@RequestHeader(value = "X-User-Id", required = false) String userId,
                      @RequestBody AskRequest req) {
        if (apiKey == null || apiKey.isBlank()) return Map.of("success", false, "error", "DASHSCOPE_API_KEY is not configured");
        if (userId == null || userId.isBlank()) return Map.of("success", false, "error", "X-User-Id header is required");
        if (req == null || req.question() == null || req.question().isBlank())
            return Map.of("success", false, "error", "question must not be blank");

        UserContext.set(userId);
        try {
            String answer = chatClient.prompt().user(req.question()).call().content();
            return Map.of("success", true, "userId", userId, "question", req.question(), "answer", answer);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            UserContext.clear();
        }
    }

    public record AskRequest(String question) {}
}
