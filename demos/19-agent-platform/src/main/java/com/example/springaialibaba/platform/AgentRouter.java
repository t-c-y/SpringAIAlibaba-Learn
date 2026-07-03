package com.example.springaialibaba.platform;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 意图路由器：让"轻量分类模型"负责选 Agent。
 *
 * 生产建议：
 * - 分类器和业务模型可以是同一个 provider 的更便宜/更快模型（比如 qwen-turbo）。
 * - 高频路径可加规则前置（关键词命中直接命中 Agent），只把兜底交给模型。
 * - 分类结果 + 置信度都要打点，形成"误路由率"指标。
 */
@Component
public class AgentRouter {

    private final ChatClient classifier;
    private final List<Agent> agents;

    public AgentRouter(ChatClient.Builder b, List<Agent> agents) {
        this.agents = agents;
        String catalog = agents.stream()
                .map(a -> "- " + a.id() + "：" + a.description())
                .reduce((x, y) -> x + "\n" + y).orElse("");
        this.classifier = b.defaultSystem("""
                你是 Agent 路由器。已注册以下 Agent：
                %s
                请把用户问题分配给最合适的 Agent，只输出 Agent id，不要输出其它内容。
                如果没有合适的，输出 fallback。
                """.formatted(catalog)).build();
    }

    public Agent route(String question) {
        String id = classifier.prompt().user(question).call().content().trim();
        return agents.stream().filter(a -> a.id().equalsIgnoreCase(id)).findFirst()
                .orElse(agents.stream().filter(a -> a.id().equals("learning")).findFirst().orElseThrow());
    }

    public List<Map<String, String>> registry() {
        return agents.stream().map(a -> Map.of("id", a.id(), "description", a.description())).toList();
    }
}
