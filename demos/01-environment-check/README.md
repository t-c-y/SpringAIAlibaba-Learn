# 01 Environment Check Demo

这是阶段一的 Spring AI Alibaba 环境检查 Demo。

本 Demo 的目标不是开发完整 ChatBot，而是先验证：

- Spring Boot 项目可以正常启动。
- Spring AI Alibaba 依赖可以正常引入。
- DashScope / 百炼 API Key 可以从环境变量读取。
- 模型名称可以通过配置指定。
- `ChatClient` 可以成功调用大模型。

---

## 一、阶段目标

阶段一：Spring AI Alibaba 基础认知与环境准备。

完成本阶段后，你应该能掌握：

1. Spring AI Alibaba 在项目中的依赖引入方式。
2. Spring AI Alibaba 与 Spring Boot 自动配置的关系。
3. DashScope / 百炼 API Key 的配置方式。
4. 模型名称的配置方式。
5. 如何通过最小接口验证模型是否可用。
6. 为什么 API Key 不能写死在代码或仓库中。

---

## 二、项目结构

```text
01-environment-check
├── README.md
├── pom.xml
└── src
    └── main
        ├── java
        │   └── com/example/springaialibaba/envcheck
        │       ├── EnvironmentCheckApplication.java
        │       └── EnvironmentCheckController.java
        └── resources
            └── application.yml
```

目录说明：

| 文件或目录 | 作用 |
| --- | --- |
| `pom.xml` | Maven 配置，声明 Spring Boot、Spring AI Alibaba 版本和依赖 |
| `application.yml` | 应用端口、应用名、DashScope API Key、模型名称配置 |
| `EnvironmentCheckApplication.java` | Spring Boot 启动类 |
| `EnvironmentCheckController.java` | 环境检查和模型调用验证接口 |

---

## 三、核心技术点

### 1. Spring Boot

用于启动 Web 服务，并提供 REST API。

### 2. Spring AI Alibaba

用于接入阿里云 DashScope / 百炼大模型服务。

### 3. DashScope / 百炼 API Key

用于访问大模型服务。

本项目通过环境变量读取：

```bash
DASHSCOPE_API_KEY
```

### 4. 模型名称

本项目通过环境变量读取模型名：

```bash
DASHSCOPE_CHAT_MODEL
```

如果不配置，默认使用：

```text
qwen3.7-max
```

### 5. ChatClient

用于发起大模型聊天请求。

本 Demo 中只用它做一次最小模型调用验证。

---

## 四、运行前准备

### 1. JDK

建议使用 JDK 17 或更高版本。

检查命令：

```bash
java -version
```

### 2. Maven

建议使用 Maven 3.8 或更高版本。

检查命令：

```bash
mvn -version
```

### 3. DashScope / 百炼 API Key

你需要准备一个可用的 DashScope / 百炼 API Key。

配置环境变量：

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

## 五、启动项目

进入当前 Demo 目录：

```bash
cd demos/01-environment-check
```

启动应用：

```bash
mvn spring-boot:run
```

启动成功后，默认端口为：

```text
8080
```

---

## 六、接口说明

### 1. 环境配置检查接口

```http
GET /env/status
```

测试命令：

```bash
curl 'http://localhost:8080/env/status'
```

返回示例：

```json
{
  "applicationName": "spring-ai-alibaba-environment-check-demo",
  "model": "qwen3.7-max",
  "apiKeyConfigured": true,
  "apiKeyPreview": "sk-* ****abcd",
  "message": "Spring AI Alibaba DashScope configuration loaded",
  "checkedAt": "2026-06-14T00:00:00Z"
}
```

字段说明：

| 字段 | 说明 |
| --- | --- |
| `applicationName` | 当前 Spring Boot 应用名称 |
| `model` | 当前配置的大模型名称 |
| `apiKeyConfigured` | 是否已经配置 API Key |
| `apiKeyPreview` | 脱敏后的 API Key 预览 |
| `message` | 环境检查说明 |
| `checkedAt` | 检查时间 |

注意：接口不会返回完整 API Key，只会返回脱敏信息。

---

### 2. 模型调用验证接口

```http
GET /env/ping
```

测试命令：

```bash
curl 'http://localhost:8080/env/ping'
```

也可以自定义验证消息：

```bash
curl 'http://localhost:8080/env/ping?message=请用一句话介绍 Spring AI Alibaba'
```

成功返回示例：

```json
{
  "success": true,
  "model": "qwen3.7-max",
  "content": "OK",
  "error": null
}
```

失败返回示例：

```json
{
  "success": false,
  "model": "qwen3.7-max",
  "content": null,
  "error": "DASHSCOPE_API_KEY is not configured"
}
```

---

## 七、超时排查

如果调用 `/env/ping` 时出现：

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
   curl 'http://localhost:8080/env/ping?message=只回答 OK'
   ```

3. 如果网络能通但仍然慢，可以调大读取超时时间：

   ```bash
   export DASHSCOPE_READ_TIMEOUT=120000
   mvn spring-boot:run
   ```

本 Demo 已将学习阶段重试次数调为 2 次，避免网络异常时一次请求卡太久。

---

## 八、学习步骤

建议按下面顺序学习本 Demo。

### Step 1：阅读 `pom.xml`

重点关注：

- Spring Boot 版本。
- Java 版本。
- `spring-ai-alibaba-bom`。
- `spring-ai-alibaba-starter-dashscope`。

理解目的：

> Spring AI Alibaba 通过 Starter 和自动配置降低模型接入成本。

---

### Step 2：阅读 `application.yml`

重点关注：

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

理解目的：

> API Key 和模型名不应该写死在代码里，应该通过环境变量或配置中心管理。

---

### Step 3：阅读启动类

文件：

```text
src/main/java/com/example/springaialibaba/envcheck/EnvironmentCheckApplication.java
```

理解目的：

> Spring AI Alibaba 应用本质上仍然是一个 Spring Boot 应用。

---

### Step 4：阅读环境检查接口

文件：

```text
src/main/java/com/example/springaialibaba/envcheck/EnvironmentCheckController.java
```

重点关注：

- 如何读取配置。
- 如何脱敏 API Key。
- 如何返回环境状态。

理解目的：

> 在真正开发 ChatBot 之前，先确认配置是否正确，可以减少排查成本。

---

### Step 5：阅读模型调用验证逻辑

重点关注：

- `ChatClient.Builder`
- `defaultSystem(...)`
- `chatClient.prompt().user(...).call().content()`
- 异常捕获和失败返回

理解目的：

> 阶段一只需要验证模型调用链路是否打通，不需要复杂 Prompt 或业务逻辑。

---

### Step 6：运行 `/env/status`

确认：

- 应用能启动。
- 模型名读取正确。
- API Key 已配置。
- API Key 没有明文暴露。

---

### Step 7：运行 `/env/ping`

确认：

- `ChatClient` 能成功调用模型。
- 网络和账号配置可用。
- 模型返回结果正常。

---

## 九、原理说明

### 1. Spring AI Alibaba 是怎么接入模型的？

本 Demo 的调用链路可以理解为：

```text
HTTP 请求
  ↓
EnvironmentCheckController
  ↓
ChatClient
  ↓
Spring AI ChatModel 抽象
  ↓
Spring AI Alibaba DashScope 实现
  ↓
DashScope / 百炼模型服务
  ↓
返回模型回答
```

关键点：

- `ChatClient` 是 Spring AI 提供的高层聊天客户端。
- Spring AI Alibaba 提供 DashScope / 百炼的具体实现。
- 你在业务代码中主要使用 `ChatClient`，不需要直接拼接底层 HTTP 请求。
- 这样做的好处是业务代码更稳定，后续切换模型或扩展能力更方便。

---

### 2. Starter 和自动配置的作用

`pom.xml` 中引入了：

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
</dependency>
```

这个 Starter 会做几件事：

1. 引入 Spring AI Alibaba 访问 DashScope 所需的依赖。
2. 注册 DashScope 相关自动配置。
3. 读取 `spring.ai.dashscope.*` 配置。
4. 创建模型调用相关 Bean。
5. 让你可以在代码中直接注入 `ChatClient.Builder`。

所以你在 Controller 中可以直接写：

```java
public EnvironmentCheckController(ChatClient.Builder chatClientBuilder, ...) {
    this.chatClient = chatClientBuilder.build();
}
```

这就是 Spring Boot 自动配置带来的便利。

---

### 3. 为什么先注入 `ChatClient.Builder`，而不是直接 new ChatClient？

因为 `ChatClient.Builder` 已经由 Spring 容器配置好了底层模型能力。

它背后包含：

- 模型客户端配置。
- API Key 配置。
- 模型名称配置。
- Spring AI 的调用抽象。
- Spring AI Alibaba 的 DashScope 实现。

如果你自己 `new`，就需要手动组装很多底层对象，不适合 Spring Boot 应用。

在 Spring 项目中，推荐做法是：

```text
让框架负责创建基础设施对象
业务代码只注入并使用这些对象
```

---

### 4. `defaultSystem` 是什么？

`defaultSystem(...)` 设置的是默认 System Prompt。

System Prompt 的作用是告诉模型：

- 你是谁。
- 你应该如何回答。
- 你有什么行为边界。
- 你的输出风格是什么。

本 Demo 中写的是：

```java
.defaultSystem("你是一个 Spring AI Alibaba 环境检查助手。回答要简洁。")
```

它的目的不是复杂 Prompt 工程，而是让你先看到：

```text
模型调用 = system 指令 + user 输入 + 模型生成
```

后续阶段会专门学习 Prompt 工程。

---

### 5. `/env/status` 和 `/env/ping` 为什么要分开？

这两个接口解决的问题不同。

#### `/env/status`

只检查本地配置，不调用模型。

适合排查：

- 应用是否启动。
- 配置是否读取。
- API Key 是否存在。
- 模型名是否正确。

#### `/env/ping`

会真正调用模型。

适合排查：

- API Key 是否有效。
- 模型服务是否开通。
- 网络是否可达。
- Spring AI Alibaba 调用链路是否正常。

分开的好处是：

```text
先排查本地配置，再排查远程模型调用
```

这样问题定位更清晰。

---

### 6. 配置读取原理

`application.yml` 中的配置：

```yaml
spring:
  ai:
    retry:
      max-attempts: 2
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:默认值}
      read-timeout: ${DASHSCOPE_READ_TIMEOUT:60000}
      chat:
        options:
          model: ${DASHSCOPE_CHAT_MODEL:qwen3.7-max}
```

含义是：

- 优先读取环境变量 `DASHSCOPE_API_KEY`。
- 如果环境变量不存在，就使用冒号后面的默认值。
- `DASHSCOPE_CHAT_MODEL` 同理。
- `DASHSCOPE_READ_TIMEOUT` 用于控制读取超时时间，默认 120000 毫秒。

Controller 中通过 `@Value` 读取配置：

```java
@Value("${spring.ai.dashscope.api-key:}") String apiKey
@Value("${spring.ai.dashscope.chat.options.model:qwen3.7-max}") String model
```

Spring Boot 启动时会把配置值注入到构造方法参数中。

---

### 7. 为什么 API Key 要脱敏？

API Key 是访问模型服务的凭证。

如果泄露，可能导致：

- 别人盗用你的模型额度。
- 产生额外费用。
- 访问你账号下的服务能力。
- 造成安全审计问题。

所以 `/env/status` 只返回：

```text
前 4 位 + **** + 后 4 位
```

不返回完整 Key。

真实生产系统中还应避免：

- 把 API Key 写入日志。
- 把 API Key 返回给前端。
- 把 API Key 提交到 Git 仓库。

---

### 8. 同步调用原理

`/env/ping` 使用的是同步调用：

```java
chatClient.prompt()
        .user(message)
        .call()
        .content();
```

可以理解为：

```text
构造 Prompt
  ↓
添加 user 消息
  ↓
发起模型调用
  ↓
等待模型完整生成
  ↓
取出文本内容
```

同步调用适合：

- 简单问答。
- 后台任务。
- 环境验证。
- 对实时逐字输出要求不高的场景。

后续 ChatBot 阶段会学习流式调用。

---

### 9. 阶段一为什么不直接做完整 ChatBot？

因为 AI 应用开发中常见问题很多：

- API Key 没配。
- 模型名写错。
- 账号没有权限。
- 网络无法访问模型服务。
- 依赖版本不匹配。
- JDK 版本不匹配。

如果一开始就写完整 ChatBot，排查问题会比较混乱。

阶段一先做环境检查 Demo，目的是建立一个最小闭环：

```text
依赖正确 → 配置正确 → 应用能启动 → 模型能调用
```

只有这个闭环稳定后，再进入 ChatBot、Prompt、Memory、Tool Calling、RAG 才更顺畅。

---

## 十、常见问题

### 1. `apiKeyConfigured` 返回 `false`

说明没有配置环境变量。

解决：

```bash
export DASHSCOPE_API_KEY=你的 API Key
```

然后重启应用。

---

### 2. `/env/ping` 返回认证失败

可能原因：

- API Key 填错。
- API Key 已失效。
- 当前账号没有模型调用权限。

处理方式：

- 检查 DashScope / 百炼控制台。
- 重新生成 API Key。
- 确认模型服务已经开通。

---

### 3. `/env/ping` 请求很慢

可能原因：

- 网络访问慢。
- 模型响应慢。
- 首次调用初始化耗时较长。

处理方式：

- 等待首次调用完成。
- 再次请求观察耗时。
- 使用 `DASHSCOPE_READ_TIMEOUT` 调大读取超时时间。
- 本 Demo 已将学习阶段重试次数调为 2 次，避免网络异常时一次请求卡太久。

---

### 4. 端口被占用

默认端口是 8080。

可以临时指定端口启动：

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

---

## 十一、阶段完成标准

完成本 Demo 后，你应该能做到：

- 能说明 Spring AI Alibaba 项目如何引入依赖。
- 能说明 DashScope API Key 如何配置。
- 能说明模型名如何配置。
- 能启动 Spring AI Alibaba 应用。
- 能通过 `/env/status` 检查配置。
- 能通过 `/env/ping` 验证模型调用。
- 能理解为什么要先做环境检查 Demo，再进入 ChatBot Demo。

---

## 十二、下一阶段

完成本 Demo 后，进入阶段二：ChatClient 与 ChatBot 入门。

下一阶段建议 Demo：

```text
demos/02-chatbot
```

学习目标：

- 使用 `ChatClient` 开发正式 ChatBot。
- 实现普通问答接口。
- 实现 SSE 流式问答接口。
- 使用 System Prompt 固定助手角色。
