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
qwen-plus
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

可选：配置模型名。

```bash
export DASHSCOPE_CHAT_MODEL=qwen-plus
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
  "model": "qwen-plus",
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
  "model": "qwen-plus",
  "content": "OK",
  "error": null
}
```

失败返回示例：

```json
{
  "success": false,
  "model": "qwen-plus",
  "content": null,
  "error": "DASHSCOPE_API_KEY is not configured"
}
```

---

## 七、学习步骤

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
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:}
      chat:
        options:
          model: ${DASHSCOPE_CHAT_MODEL:qwen-plus}
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

## 八、常见问题

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
- 后续阶段再学习超时和重试配置。

---

### 4. 端口被占用

默认端口是 8080。

可以临时指定端口启动：

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

---

## 九、阶段完成标准

完成本 Demo 后，你应该能做到：

- 能说明 Spring AI Alibaba 项目如何引入依赖。
- 能说明 DashScope API Key 如何配置。
- 能说明模型名如何配置。
- 能启动 Spring AI Alibaba 应用。
- 能通过 `/env/status` 检查配置。
- 能通过 `/env/ping` 验证模型调用。
- 能理解为什么要先做环境检查 Demo，再进入 ChatBot Demo。

---

## 十、下一阶段

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
