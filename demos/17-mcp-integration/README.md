# 17 MCP 工具集成 Demo

阶段十七目标：理解 **Model Context Protocol** 的思想——工具不是编译期注册的 `@Tool`，而是通过标准协议**动态发现和调用**。

## 一、为什么用 MCP

`@Tool` 注册的工具必须在你自己的进程里；MCP 让工具服务可以是：

- 别的团队维护的独立服务
- 甚至别的语言（Python / Node）写的工具
- 加载后即用，模型只关心 tool schema

## 二、教学取舍

Spring AI Alibaba 1.0.0.2 的 MCP 集成仍在演进，官方 SDK 使用 JSON-RPC over stdio/SSE。本课**用 REST 模拟**协议，让你抓住核心概念：

1. `GET /mcp/tools` 拉工具清单
2. `POST /mcp/call` 按 `{toolName, arguments}` 调用

真实 MCP 协议接口形状极为相似，只是传输层换成 JSON-RPC。等官方 SDK 稳定，你只需把 `McpClient` 换成 SDK。

## 三、目录

- `MockMcpServer.java`：模拟 MCP Server，提供 `list_files` 和 `search_docs` 两个工具。
- `McpClient.java`：客户端，用 RestClient 消费上面两个接口。
- `McpAgentController.java`：把工具清单拼进 System Prompt，用"JSON 协议"和模型循环互动。

## 四、运行

```bash
cd demos/17-mcp-integration
export DASHSCOPE_API_KEY=你的 API Key
mvn spring-boot:run

# 查看工具清单
curl 'http://localhost:8096/mcp/tools'

# 直接调工具（不经模型）
curl -X POST 'http://localhost:8096/mcp/call' -H 'Content-Type: application/json' \
  -d '{"toolName":"search_docs","arguments":{"q":"advisor"}}'

# 让 Agent 帮你用工具
curl -X POST 'http://localhost:8096/mcp-agent/ask' -H 'Content-Type: application/json' \
  -d '{"question":"帮我列出根目录的文件"}'

curl -X POST 'http://localhost:8096/mcp-agent/ask' -H 'Content-Type: application/json' \
  -d '{"question":"搜一下关键字 tool，最后用一句话总结"}'
```

## 五、学习检查点

- [ ] 能说清楚 MCP 相较 `@Tool` 的核心差异（动态发现、跨进程、跨语言）。
- [ ] 能画出 Agent-LLM-MCP Server 之间的时序图。
- [ ] 明白"响应协议"里让模型只输出 JSON 的重要性——否则 Agent 无法解析。
- [ ] 能识别循环控制点：最多 N 轮、明确终止条件。

## 六、生产扩展方向

- 换成官方 MCP SDK 的 stdio / SSE 通道。
- Server 端加鉴权、限流、审计。
- Agent 侧对工具调用做 dryRun / dryRun-preview（写操作前让人确认）。
