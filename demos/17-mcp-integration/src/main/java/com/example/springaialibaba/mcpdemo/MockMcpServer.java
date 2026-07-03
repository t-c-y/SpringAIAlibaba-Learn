package com.example.springaialibaba.mcpdemo;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 模拟 MCP 工具服务器。
 *
 * 关键接口（简化版 MCP）：
 * - GET  /mcp/tools           返回工具清单 (name, description, inputSchema)
 * - POST /mcp/call            {toolName, arguments} → 结构化结果
 *
 * 真实 MCP 使用 JSON-RPC over stdio / SSE / websocket，工具协议大同小异：先“列表工具”，再“调用工具”。
 * 教学阶段用普通 REST，便于观察和调试。
 */
@RestController
@RequestMapping("/mcp")
public class MockMcpServer {

    @GetMapping("/tools")
    public Object tools() {
        return List.of(
                Map.of(
                        "name", "list_files",
                        "description", "列出模拟磁盘上的文件。可选参数 dir，默认 '/'。",
                        "inputSchema", Map.of("type", "object",
                                "properties", Map.of("dir", Map.of("type", "string")))),
                Map.of(
                        "name", "search_docs",
                        "description", "在模拟知识库中做关键字检索，返回命中的文档标题和摘要。参数 q 是关键字。",
                        "inputSchema", Map.of("type", "object",
                                "properties", Map.of("q", Map.of("type", "string")),
                                "required", List.of("q")))
        );
    }

    @PostMapping("/call")
    public Object call(@RequestBody CallRequest req) {
        return switch (req.toolName() == null ? "" : req.toolName()) {
            case "list_files" -> listFiles(String.valueOf(req.arguments().getOrDefault("dir", "/")));
            case "search_docs" -> searchDocs(String.valueOf(req.arguments().getOrDefault("q", "")));
            default -> Map.of("ok", false, "error", "unknown tool: " + req.toolName());
        };
    }

    private Object listFiles(String dir) {
        Map<String, List<Map<String, Object>>> fs = Map.of(
                "/", List.of(
                        Map.of("name", "README.md", "size", 1024, "modifiedAt", LocalDate.of(2026, 7, 1)),
                        Map.of("name", "notes", "type", "dir")),
                "/notes", List.of(
                        Map.of("name", "spring-ai.md", "size", 2048, "modifiedAt", LocalDate.of(2026, 6, 30)))
        );
        List<Map<String, Object>> res = fs.getOrDefault(dir, List.of());
        return Map.of("ok", true, "dir", dir, "files", res);
    }

    private Object searchDocs(String q) {
        Map<String, String> docs = Map.of(
                "chatclient", "ChatClient 是业务代码调用大模型的主要入口。",
                "advisor", "Advisor 允许你在调用前后拦截 Prompt / Response，实现记忆、RAG 等能力。",
                "tool", "Tool Calling 让模型调用你写的 Java 方法。"
        );
        var hits = docs.entrySet().stream()
                .filter(e -> q != null && !q.isBlank() && e.getKey().contains(q.toLowerCase()))
                .map(e -> Map.of("title", e.getKey(), "abstract", e.getValue()))
                .toList();
        return Map.of("ok", true, "q", q, "hits", hits);
    }

    public record CallRequest(String toolName, Map<String, Object> arguments) {}
}
