# 03 Streaming ChatBot Demo

这是阶段三的 Spring AI Alibaba 流式 ChatBot Demo。

阶段二 `02-chatbot` 已经实现了同步问答接口。阶段三开始学习如何让模型回答边生成边返回，也就是常见 ChatBot 的“打字机效果”。

---

## 一、阶段目标

完成本 Demo 后，你应该能掌握：

1. 如何使用 `ChatClient` 实现流式问答。
2. 如何通过 SSE 返回模型生成过程中的文本片段。
3. `.call().content()` 和 `.stream().content()` 的区别。
4. 为什么长回答场景需要流式输出。
5. 为什么本阶段仍然不引入 Memory、Tool Calling、RAG。

---

## 二、项目结构

```text
03-streaming-chatbot
├── README.md
├── pom.xml
└── src
    └── main
        ├── java
        │   └── com/example/springaialibaba/streamingchatbot
        │       ├── DashScopeHttpClientConfiguration.java
        │       ├── StreamingChatController.java
        │       └── StreamingChatbotApplication.java
        └── resources
            └── application.yml
```

---

## 三、核心调用链路

```text
HTTP POST /chat/stream
  ↓
StreamingChatController
  ↓
ChatClient.prompt()
  ↓
user question
  ↓
.stream()
  ↓
.content()
  ↓
Flux<String>
  ↓
text/event-stream
```

本阶段要记住一句话：

> 同步接口等待完整回答后一次性返回；流式接口把模型生成过程持续返回给客户端。

---

## 四、运行前准备

配置 API Key：

```bash
export DASHSCOPE_API_KEY=你的 API Key
```

可选：配置模型名和读取超时时间。

```bash
export DASHSCOPE_CHAT_MODEL=qwen3.7-max
export DASHSCOPE_READ_TIMEOUT=180000
```

如果本机访问 DashScope 较慢，可以继续调大读取超时时间：

```bash
export DASHSCOPE_READ_TIMEOUT=240000
```

---

## 五、启动应用

进入当前 Demo 目录：

```bash
cd demos/03-streaming-chatbot
```

启动：

```bash
mvn spring-boot:run
```

默认端口：

```text
8082
```

---

## 六、接口说明

### 1. ChatBot 状态接口

```http
GET /chat/status
```

测试：

```bash
curl 'http://localhost:8082/chat/status'
```

示例返回：

```json
{
  "model": "qwen3.7-max",
  "apiKeyConfigured": true,
  "apiKeyPreview": "sk-* ****abcd",
  "mode": "streaming-sse",
  "message": "Streaming ChatBot demo is ready",
  "checkedAt": "2026-06-20T00:00:00Z"
}
```

---

### 2. 同步问答接口

```http
POST /chat/ask
Content-Type: application/json

{"question":"为什么 ChatBot 需要流式输出？"}
```

测试：

```bash
curl -X POST 'http://localhost:8082/chat/ask' \
  -H 'Content-Type: application/json' \
  -d '{"question":"请用三点说明为什么 ChatBot 需要流式输出"}'
```

这个接口会等待模型完整生成，然后一次性返回 JSON。

---

### 3. 流式问答接口

```http
POST /chat/stream
Content-Type: application/json
Accept: text/event-stream

{"question":"请用三点解释 SSE 为什么适合 ChatBot"}
```

测试：

```bash
curl -N -X POST 'http://localhost:8082/chat/stream' \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  -d '{"question":"请用三点解释 SSE 为什么适合 ChatBot"}'
```

注意：`-N` 用于关闭 curl 缓冲。没有 `-N` 时，终端可能看起来不像逐步输出。

---

## 七、同步 vs 流式对比

### 同步调用

```java
String answer = chatClient.prompt()
        .user(request.question())
        .call()
        .content();
```

特点：

- 等模型完整生成后返回。
- Controller 返回普通 JSON。
- 适合短回答、后台任务、环境验证。

### 流式调用

```java
Flux<String> answerStream = chatClient.prompt()
        .user(request.question())
        .stream()
        .content();
```

特点：

- 模型生成一部分，就可以返回一部分。
- Controller 返回 `text/event-stream`。
- 适合长回答、智能客服、代码解释、文档总结。

---

## 八、流式输出排查

### 1. 看起来没有流式效果

优先确认 curl 命令使用了：

```bash
-N
```

并且请求头包含：

```bash
-H 'Accept: text/event-stream'
```

### 2. 调用超时

先测试 DashScope 域名连通性：

```bash
curl -I --connect-timeout 5 https://dashscope.aliyuncs.com
```

如果网络能通但模型响应慢，可以调大：

```bash
export DASHSCOPE_READ_TIMEOUT=240000
```

然后重启应用。

### 3. 返回不是 JSON

这是正常的。

`/chat/ask` 返回 JSON，`/chat/stream` 返回的是 SSE 文本流：

```http
Content-Type: text/event-stream
```

---

## 九、代码阅读顺序

### Step 1：阅读 `application.yml`

重点关注：

```yaml
server:
  port: 8082

spring:
  ai:
    retry:
      max-attempts: 2
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:}
      read-timeout: ${DASHSCOPE_READ_TIMEOUT:180000}
      chat:
        options:
          model: ${DASHSCOPE_CHAT_MODEL:qwen3.7-max}
```

理解目的：

> 流式输出会保持更久连接，所以读取超时时间通常要比普通同步接口更宽松。

---

### Step 2：阅读 `DashScopeHttpClientConfiguration`

重点关注：

- `RestClientCustomizer`
- `OkHttpClient.Builder`
- `connectTimeout`
- `readTimeout`
- `writeTimeout`

理解目的：

> Spring AI Alibaba 底层通过 HTTP 调用 DashScope，流式场景需要更明确的 HTTP 超时配置。

---

### Step 3：阅读同步接口 `/chat/ask`

重点关注：

```java
chatClient.prompt()
        .user(request.question())
        .call()
        .content();
```

理解目的：

> 这是阶段二已经学过的一次性返回方式。

---

### Step 4：阅读流式接口 `/chat/stream`

重点关注：

```java
chatClient.prompt()
        .user(request.question())
        .stream()
        .content();
```

理解目的：

> `.stream()` 表示不等待完整回答，而是拿到一个可以持续输出的 `Flux<String>`。

---

## 十、为什么本阶段不做多轮对话？

多轮对话需要 Memory 或手动维护历史消息，会引入会话 ID、用户隔离、历史窗口和敏感信息过滤等问题。

本阶段只学习一个能力：

```text
单轮问题如何流式返回
```

等流式输出掌握后，再进入 Prompt、Memory、Tool Calling、RAG 会更清晰。

---

## 十一、阶段完成标准

你完成本阶段后，应该能做到：

- 能启动 `03-streaming-chatbot`。
- 能调用 `/chat/status` 检查配置。
- 能调用 `/chat/ask` 获得完整 JSON。
- 能用 `curl -N` 调用 `/chat/stream` 看到流式输出。
- 能解释 `.call().content()` 和 `.stream().content()` 的区别。
- 能说明为什么 SSE 适合 ChatBot 的单向流式输出。

---

## 十二、下一阶段

完成本 Demo 后，进入阶段四：Prompt 学习助手 Demo。

下一阶段建议 Demo：

```text
demos/04-prompt-learning-assistant
```

学习目标：

- 使用 System Prompt 控制助手角色。
- 使用 User Prompt 表达用户任务。
- 设计更稳定、更可复用的 Prompt 模式。
