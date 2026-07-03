package com.example.springaialibaba.mcpdemo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * MCP Agent Controller。
 *
 * 教学流程：
 * 1. 启动时调用 McpClient.listTools() 拿到工具清单，拼进 System Prompt，告诉模型"你有哪些工具"。
 * 2. 模型输出应严格符合 {"tool":"...", "args":{...}} 结构（也可以返回 {"final":"..."} 直接给最终答复）。
 * 3. Agent 解析该 JSON，调用 McpClient.call(...)，把结果回填给模型，进入下一轮。
 * 4. 最多循环 N 次，避免死循环。
 */
@RestController
public class McpAgentController {

    private final ChatClient chat;
    private final McpClient mcp;
    private final String apiKey;

    public McpAgentController(ChatClient.Builder builder,
                              McpClient mcp,
                              @Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        this.mcp = mcp;
        this.apiKey = apiKey;

        // 拉取工具描述，拼进 System Prompt。
        String toolsDesc;
        try {
            toolsDesc = mcp.listTools().toString();
        } catch (Exception e) {
            toolsDesc = "(尚未连接到 MCP Server，请先启动 MockMcpServer)";
        }

        this.chat = builder
                .defaultSystem("""
                        你是一个通过 MCP 协议使用外部工具的助手。你只能调用下列工具，任何其它工具名都无效。

                        【可用工具】
                        %s

                        【响应协议】
                        每一步只能输出以下两种 JSON 之一，且不要添加多余文本：
                        - 调用工具：{"tool":"<toolName>","args":{...}}
                        - 结束对话：{"final":"给用户的中文回答"}

                        规则：
                        1. 不确定就直接返回 final 并说明"不确定"。
                        2. 收到工具返回后，可以继续调用工具或给出 final。
                        3. 只处理与工具能力相关的问题，其它问题用 final 礼貌拒绝。
                        """.formatted(toolsDesc))
                .build();
    }

    @PostMapping("/mcp-agent/ask")
    public Object ask(@RequestBody AskRequest req) {
        if (apiKey == null || apiKey.isBlank()) return Map.of("error", "DASHSCOPE_API_KEY is not configured");

        StringBuilder ctx = new StringBuilder("用户问题：").append(req.question());
        for (int i = 0; i < 6; i++) {
            String out = chat.prompt().user(ctx.toString()).call().content().trim();
            Map<?, ?> parsed = parseJson(out);
            if (parsed == null) {
                return Map.of("error", "LLM did not return valid JSON", "raw", out);
            }
            if (parsed.containsKey("final")) {
                return Map.of("success", true, "steps", i, "answer", parsed.get("final"));
            }
            String tool = String.valueOf(parsed.get("tool"));
            Object argsRaw = parsed.get("args");
            @SuppressWarnings("unchecked")
            Map<String, Object> args = argsRaw instanceof Map<?, ?> m
                    ? (Map<String, Object>) m
                    : new java.util.HashMap<>();
            Object result = mcp.call(tool, args);
            ctx.append("\n\n【工具 ").append(tool).append(" 返回】").append(result)
                    .append("\n请继续按响应协议输出下一步。");
        }
        return Map.of("error", "reached step limit without final answer");
    }

    /** 极简 JSON 解析（依赖 Jackson，Spring Boot 已内置）。 */
    private static Map<?, ?> parseJson(String s) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(s, Map.class);
        } catch (Exception e) {
            // 兼容模型偶尔用 ```json``` 包裹的场景
            int start = s.indexOf('{');
            int end = s.lastIndexOf('}');
            if (start >= 0 && end > start) {
                try {
                    return new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(s.substring(start, end + 1), Map.class);
                } catch (Exception ignore) { }
            }
            return null;
        }
    }

    public record AskRequest(String question) {}
}
