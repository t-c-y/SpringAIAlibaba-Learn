package com.example.springaialibaba.obs;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent 观测 + 评测 Controller。
 *
 * 两条主线：
 * 1. /obs/ask   记录每次交互的 latency、estimatedTokens、question、answer，可通过 /obs/logs 查询。
 * 2. /obs/eval  用一份"标准问题-标准答案"数据集，跑 LLM-as-a-judge 打分，输出准确率。
 */
@RestController
public class ObservabilityController {

    private final ChatClient user;
    private final ChatClient judge;
    private final InteractionLogStore store;
    private final String apiKey;

    public ObservabilityController(ChatClient.Builder builder,
                                   InteractionLogStore store,
                                   @Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        this.store = store;
        this.apiKey = apiKey;
        this.user = builder.defaultSystem("你是简洁的技术助手，先给结论再给依据。中文回答。").build();
        this.judge = builder.defaultSystem("""
                你是严格的答案评审员。给定"标准答案"和"待评答案"，判定待评答案是否语义等价、无编造。
                只输出以下 JSON 之一：{"pass":true} 或 {"pass":false,"reason":"..."}。不要输出其它文字。
                """).build();
    }

    @PostMapping("/obs/ask")
    public Object ask(@RequestBody AskRequest req) {
        if (apiKey == null || apiKey.isBlank()) return Map.of("error", "DASHSCOPE_API_KEY is not configured");
        long t0 = System.nanoTime();
        String answer = user.prompt().user(req.question()).call().content();
        long ms = (System.nanoTime() - t0) / 1_000_000L;
        // 极简的“字符数≈4 字节 UTF-8≈token 上限”估算，仅供教学参考。
        int estTokens = (req.question().length() + answer.length()) / 3;
        String id = store.log(InteractionLogStore.newEntry(req.userId(), req.question(), answer, ms, estTokens));
        return Map.of("id", id, "answer", answer, "latencyMs", ms, "estTokens", estTokens);
    }

    @GetMapping("/obs/logs")
    public Object logs(@RequestParam(defaultValue = "20") int n) {
        return store.tail(n);
    }

    /** 数据集式评测：跑完一批标准问题后，用 LLM 作为评审给每条打分。 */
    @PostMapping("/obs/eval")
    public Object eval(@RequestBody EvalRequest req) {
        if (apiKey == null || apiKey.isBlank()) return Map.of("error", "DASHSCOPE_API_KEY is not configured");
        int pass = 0;
        List<Map<String, Object>> details = new java.util.ArrayList<>();
        for (EvalCase c : req.cases()) {
            String answer = user.prompt().user(c.question()).call().content();
            String verdict = judge.prompt().user("""
                    标准答案：%s
                    待评答案：%s
                    """.formatted(c.expected(), answer)).call().content().trim();
            boolean ok = verdict.contains("\"pass\":true") || verdict.contains("\"pass\": true");
            if (ok) pass++;
            details.add(Map.of("question", c.question(), "expected", c.expected(),
                    "answer", answer, "verdict", verdict, "pass", ok));
        }
        double rate = req.cases().isEmpty() ? 0.0 : (double) pass / req.cases().size();
        return Map.of("passCount", pass, "total", req.cases().size(),
                "passRate", rate, "details", details);
    }

    public record AskRequest(String userId, String question) {}
    public record EvalRequest(List<EvalCase> cases) {}
    public record EvalCase(String question, String expected) {}
}
