# 02 ChatBot Demo

这是阶段二的 Spring AI Alibaba 最小 ChatBot Demo。

阶段一 `01-environment-check` 只证明环境和模型调用链路可用。阶段二开始把模型调用包装成一个真正的后端业务接口。

---

## 一、阶段目标

完成本 Demo 后，你应该能掌握：

1. 如何用 `ChatClient` 实现一个同步问答接口。
2. `defaultSystem(...)` 和 `.user(...)` 在一次模型调用中的分工。
3. 为什么企业项目中要先把模型调用封装在 Controller / Service 边界内。
4. 如何用状态接口排查 API Key 和模型名配置。
5. 为什么本阶段暂时不引入 Memory、Tool Calling、RAG。

---

## 二、项目结构

```text
02-chatbot
├── README.md
├── pom.xml
└── src
    └── main
        ├── java
        │   └── com/example/springaialibaba/chatbot
        │       ├── ChatbotApplication.java
        │       └── ChatController.java
        └── resources
            └── application.yml
```

---

## 三、核心调用链路

```text
HTTP POST /chat/ask
  ↓
ChatController
  ↓
ChatClient.prompt()
  ↓
defaultSystem + user question
  ↓
.call()
  ↓
DashScope / 百炼模型
  ↓
.content()
  ↓
JSON response
```

本阶段要记住一句话：

> `ChatClient` 是应用代码进入大模型能力的入口；`System Prompt` 管行为边界，`User Message` 放用户本次问题。

---

## 四、运行前准备

配置 API Key：

```bash
export DASHSCOPE_API_KEY=你的 API Key
```

可选：配置模型名和读取超时时间。

```bash
export DASHSCOPE_CHAT_MODEL=qwen3.7-max
export DASHSCOPE_READ_TIMEOUT=120000
```

如果本机访问 DashScope 较慢，可以临时调大超时时间：

```bash
export DASHSCOPE_READ_TIMEOUT=120000
```

---

## 五、启动应用

进入当前 Demo 目录：

```bash
cd demos/02-chatbot
```

启动：

```bash
mvn spring-boot:run
```

默认端口：

```text
8081
```

---

## 六、接口说明

### 1. ChatBot 状态接口

```http
GET /chat/status
```

测试：

```bash
curl 'http://localhost:8081/chat/status'
```

示例返回：

```json
{
  "model": "qwen3.7-max",
  "apiKeyConfigured": true,
  "apiKeyPreview": "sk-* ****abcd",
  "message": "ChatBot demo is ready",
  "checkedAt": "2026-06-19T00:00:00Z"
}
```

---

### 2. 同步问答接口

```http
POST /chat/ask
Content-Type: application/json

{"question":"Spring AI Alibaba 适合解决什么问题？"}
```

测试：

```bash
curl -X POST 'http://localhost:8081/chat/ask' \
  -H 'Content-Type: application/json' \
  -d '{"question":"请用三点说明 Spring AI Alibaba 在企业开发中的价值"}'
```

示例返回：

```json
{
  "success": true,
  "model": "qwen3.7-max",
  "question": "请用三点说明 Spring AI Alibaba 在企业开发中的价值",
  "answer": "...",
  "error": null
}
```

---

## 七、超时排查

如果调用 `/chat/ask` 时出现：

```text
I/O error on POST request for "https://dashscope.aliyuncs.com/...": timeout
```

说明应用已经读到了 API Key，也已经向 DashScope 发起请求，但等待模型响应时超时。

建议按顺序排查：

1. 先测试本机网络是否能访问 DashScope：

   ```bash
   curl -I --connect-timeout 5 https://dashscope.aliyuncs.com
   ```

2. 用最短问题测试，排除长回答导致的等待：

   ```bash
   curl -X POST 'http://localhost:8081/chat/ask' \
     -H 'Content-Type: application/json' \
     -d '{"question":"只回答 OK"}'
   ```

3. 如果网络能通但仍然慢，可以调大读取超时时间：

   ```bash
   export DASHSCOPE_READ_TIMEOUT=120000
   mvn spring-boot:run
   ```

本 Demo 已将学习阶段重试次数调为 2 次，避免网络异常时一次请求卡太久。

---

## 八、代码阅读顺序

### Step 1：看 `application.yml`

重点看：

```yaml
spring:
  ai:
    retry:
      max-attempts: 2
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:}
      read-timeout: ${DASHSCOPE_READ_TIMEOUT:60000}
      chat:
        options:
          model: ${DASHSCOPE_CHAT_MODEL:qwen3.7-max}
```

你要形成习惯：

> 模型服务凭证通过环境变量或密钥系统注入，不写死在仓库中。

---

### Step 2：看构造方法中的 `defaultSystem`

```java
this.chatClient = chatClientBuilder
        .defaultSystem("...")
        .build();
```

`defaultSystem` 的职责是给模型设定默认身份和回答规则。它类似“后端服务的默认策略”，而不是用户本次提问。

---

### Step 3：看 `/chat/ask`

```java
String answer = chatClient.prompt()
        .user(request.question())
        .call()
        .content();
```

拆开理解：

| 代码 | 含义 |
| --- | --- |
| `prompt()` | 开始构造一次提示词请求 |
| `user(...)` | 放入用户本次问题 |
| `call()` | 发起同步模型调用 |
| `content()` | 取出模型文本回答 |

---

## 九、为什么本阶段不做多轮对话？

因为多轮对话需要引入 Memory 或手动维护历史消息。企业开发中，Memory 还会牵涉：

- 会话 ID。
- 用户隔离。
- 历史窗口长度。
- 敏感信息过滤。
- 存储成本。

这些会在后续阶段单独学习。本阶段先把“单轮模型调用”练熟。

---

## 十、阶段完成标准

你完成本阶段后，应该能做到：

- 能启动 `02-chatbot`。
- 能调用 `/chat/status` 检查配置。
- 能调用 `/chat/ask` 获得模型回答。
- 能解释 `defaultSystem`、`user`、`call`、`content` 分别做什么。
- 能说出为什么 API Key 不能写死在配置文件中。

---

## 十一、下一阶段

下一阶段建议进入：

```text
demos/03-streaming-chatbot
```

学习目标：

- 使用 SSE 返回流式模型输出。
- 理解同步回答和流式回答的体验差异。
- 为企业 ChatBot 前端交互打基础。
