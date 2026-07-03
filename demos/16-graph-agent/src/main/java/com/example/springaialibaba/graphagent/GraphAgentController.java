package com.example.springaialibaba.graphagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 教学版 Graph Runtime：手写一个"节点 map + 迁移函数"的最小 Graph。
 *
 * 你会看到几个关键概念：
 * 1. 节点（Node）是一个 Function<ChatState, String>，返回下一节点名。
 * 2. 边（Edge）通过节点返回值 + edges map 表达。
 * 3. 状态（State）在 ChatState 里显式管理，可断点续跑、可打日志、可回放。
 *
 * 相较于"一个 ChatClient 塞所有 Advisor + Tool"，Graph 的优势：
 * - 每个节点做一件事，出问题好定位；
 * - 转移条件是"代码"而不是"模型自由发挥"，可测试；
 * - 关键节点（例如 refund）可以插入人工审批步骤。
 */
@RestController
@Component
public class GraphAgentController {

    private final ChatClient classifier;
    private final ChatClient answerer;
    private final String apiKey;

    public GraphAgentController(ChatClient.Builder builder,
                                @Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        // 一个纯做意图识别，输出必须严格是枚举字符串
        this.classifier = builder
                .defaultSystem("""
                        你是意图分类器。只输出以下枚举之一，不要输出多余文字：
                        ORDER_QUERY / POLICY_QA / REFUND / SMALL_TALK / UNKNOWN
                        判断规则：
                        - 涉及"我的订单/物流/发货"→ ORDER_QUERY
                        - 涉及"退款/退货/政策/保修"→ POLICY_QA
                        - 明确要求发起退款申请 → REFUND
                        - 打招呼/闲聊 → SMALL_TALK
                        - 无法判断 → UNKNOWN
                        """)
                .build();
        // 另一个专职生成中文回答，保持与业务无耦合
        this.answerer = builder
                .defaultSystem("你是简洁的中文助手，按提示词要求输出，不要添加额外解释。")
                .build();
    }

    @PostMapping("/graph/ask")
    public Object ask(@RequestHeader(value = "X-User-Id", required = false) String userId,
                      @RequestBody AskRequest req) {
        if (apiKey == null || apiKey.isBlank()) return Map.of("error", "DASHSCOPE_API_KEY is not configured");
        if (userId == null || userId.isBlank()) return Map.of("error", "X-User-Id header required");
        if (req == null || req.question() == null) return Map.of("error", "question required");

        ChatState state = new ChatState();
        state.userId = userId;
        state.userInput = req.question();
        state.node = "CLASSIFY";

        // 记录节点轨迹，方便观察 Graph 走了哪些边。
        List<String> trace = new java.util.ArrayList<>();
        int guard = 0;
        while (!state.finished && guard++ < 16) {
            trace.add(state.node);
            state.node = step(state);
            if ("END".equals(state.node)) state.finished = true;
        }
        trace.add("END");

        return Map.of("userId", userId, "trace", trace,
                "intent", state.intent, "vars", state.vars,
                "answer", state.finalAnswer());
    }

    /** 节点分发：把当前 state.node 转到对应的 handler，返回下一节点名。 */
    private String step(ChatState s) {
        return switch (s.node) {
            case "CLASSIFY" -> classify(s);
            case "ORDER_QUERY" -> orderQuery(s);
            case "POLICY_QA" -> policyQa(s);
            case "REFUND_CONFIRM" -> refundConfirm(s);
            case "SMALL_TALK" -> smallTalk(s);
            case "FALLBACK" -> fallback(s);
            default -> "END";
        };
    }

    // ---------- 节点实现 ----------

    private String classify(ChatState s) {
        String out = classifier.prompt().user(s.userInput).call().content().trim();
        s.intent = List.of("ORDER_QUERY", "POLICY_QA", "REFUND", "SMALL_TALK").contains(out) ? out : "UNKNOWN";
        return switch (s.intent) {
            case "ORDER_QUERY" -> "ORDER_QUERY";
            case "POLICY_QA" -> "POLICY_QA";
            case "REFUND" -> "REFUND_CONFIRM";
            case "SMALL_TALK" -> "SMALL_TALK";
            default -> "FALLBACK";
        };
    }

    /** 教学 Mock：真实项目里改成调订单服务。 */
    private String orderQuery(ChatState s) {
        Map<String, List<Map<String, Object>>> db = Map.of(
                "u001", List.of(
                        Map.of("orderId", "O-1001", "item", "MacBook Pro 14", "status", "已发货"),
                        Map.of("orderId", "O-1002", "item", "AirPods Pro", "status", "已签收")),
                "u002", List.of(Map.of("orderId", "O-2001", "item", "iPad Air", "status", "已支付")));
        var orders = db.getOrDefault(s.userId, List.of());
        s.vars.put("orders", orders);
        String prompt = "把下面这份订单列表用一句话总结给用户看：" + orders;
        s.append(answerer.prompt().user(prompt).call().content());
        return "END";
    }

    private String policyQa(ChatState s) {
        // 教学阶段用一份内嵌的政策条款，避免额外引入 VectorStore
        String policy = """
                退款政策要点：
                1. 未发货订单 24 小时内自助取消，全额原路退。
                2. 已发货未签收可拦截退货，7 个工作日内退款。
                3. 已签收 7 天内无理由退货（商品完好，运费买家承担）。
                4. 已签收超过 7 天需质量鉴定通过后退款。
                5. 定制商品不支持无理由退货。
                """;
        String user = "参考以下政策，用中文回答用户的问题。政策：\n" + policy + "\n用户问题：" + s.userInput;
        s.append(answerer.prompt().user(user).call().content());
        return "END";
    }

    /** 退款节点显式要求人工确认，展示"关键动作走审批"的编排思路。 */
    private String refundConfirm(ChatState s) {
        s.vars.put("pendingRefund", Map.of("requestBy", s.userId, "rawInput", s.userInput));
        s.append("已识别到退款诉求，退款申请已进入人工审批队列，客服将在 24 小时内联系您确认。此过程不会自动打款。");
        return "END";
    }

    private String smallTalk(ChatState s) {
        s.append(answerer.prompt().user("请用一句友好的话回应用户：" + s.userInput).call().content());
        return "END";
    }

    private String fallback(ChatState s) {
        s.append("我目前只处理订单查询、售后政策和退款申请，其它问题请联系人工客服。");
        return "END";
    }

    public record AskRequest(String question) {}
}
