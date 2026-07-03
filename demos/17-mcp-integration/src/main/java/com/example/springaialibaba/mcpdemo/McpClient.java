package com.example.springaialibaba.mcpdemo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * MCP 客户端。
 *
 * 与"注解式 Tool"的最大差异：工具不再是编译期已知的 @Tool 方法，而是通过 HTTP 从服务端动态发现，
 * 每个工具的 schema 只在运行时才知道。真实 MCP 用 JSON-RPC；本课用普通 REST 表达相同思路。
 */
@Component
public class McpClient {

    private final RestClient http;

    public McpClient(@Value("${mcp.server.baseUrl:http://localhost:8096}") String baseUrl) {
        this.http = RestClient.create(baseUrl);
    }

    public List<Map<String, Object>> listTools() {
        return http.get().uri("/mcp/tools").retrieve().body(List.class);
    }

    public Object call(String toolName, Map<String, Object> args) {
        return http.post().uri("/mcp/call")
                .body(Map.of("toolName", toolName, "arguments", args == null ? Map.of() : args))
                .retrieve().body(Object.class);
    }
}
