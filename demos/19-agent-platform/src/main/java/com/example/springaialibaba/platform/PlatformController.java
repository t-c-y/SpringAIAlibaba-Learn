package com.example.springaialibaba.platform;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 平台统一入口 Controller。
 *
 * 步骤：
 *   ① 鉴权 & 限流（RequestGuard）
 *   ② 路由到具体 Agent（AgentRouter）
 *   ③ 执行 Agent 并记录审计（AuditLog）
 *   ④ 返回统一响应结构
 */
@RestController
public class PlatformController {

    private final AgentRouter router;
    private final RequestGuard guard;
    private final String apiKey;
    private final ConcurrentLinkedDeque<Map<String, Object>> audit = new ConcurrentLinkedDeque<>();

    public PlatformController(AgentRouter router, RequestGuard guard,
                              @Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        this.router = router;
        this.guard = guard;
        this.apiKey = apiKey;
    }

    @GetMapping("/platform/agents")
    public Object agents() { return router.registry(); }

    @PostMapping("/platform/ask")
    public Object ask(@RequestHeader(value = "X-User-Id", required = false) String userId,
                      @RequestBody AskRequest req) {
        if (apiKey == null || apiKey.isBlank()) return Map.of("error", "DASHSCOPE_API_KEY is not configured");
        if (userId == null || userId.isBlank()) return Map.of("error", "X-User-Id header required");
        if (req == null || req.question() == null || req.question().isBlank())
            return Map.of("error", "question required");

        if (!guard.tryAcquire(userId)) {
            return Map.of("error", "rate limited: 20 requests / minute", "usage", guard.usage(userId));
        }

        Agent agent = router.route(req.question());
        Agent.AgentResponse resp = agent.handle(new Agent.AgentRequest(userId, req.question()));

        Map<String, Object> record = Map.of(
                "id", UUID.randomUUID().toString(),
                "at", Instant.now().toString(),
                "userId", userId,
                "agentId", agent.id(),
                "latencyMs", resp.latencyMs(),
                "question", req.question(),
                "answer", resp.answer()
        );
        if (audit.size() > 500) audit.pollFirst();
        audit.addLast(record);

        return Map.of("success", true, "routedAgent", agent.id(),
                "latencyMs", resp.latencyMs(), "answer", resp.answer());
    }

    @GetMapping("/platform/audit")
    public Object audit(@RequestParam(defaultValue = "20") int n) {
        var arr = audit.toArray(new Map[0]);
        int from = Math.max(0, arr.length - n);
        return java.util.Arrays.copyOfRange(arr, from, arr.length);
    }

    public record AskRequest(String question) {}
}
