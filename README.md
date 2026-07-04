# Spring AI Alibaba AI Agent 学习 Demo

这是一个面向 **Java / Spring Boot 开发者** 的 Spring AI Alibaba 学习项目。

项目目标是帮助已经具备 Java 技术栈基础的开发者，系统学习如何使用 **Spring AI Alibaba** 开发 AI Agent，并逐步掌握 ChatBot、Prompt、Memory、Tool Calling、RAG、Graph、MCP、评测观测和企业级 Agent 架构设计。

> 本项目不再从 Java 或 Spring Boot 基础开始，而是直接围绕 Spring AI Alibaba 和 AI Agent 开发进行阶段化实践。

---

## 📚 课程目录（推荐入口）

项目内置一套可直接在浏览器打开的**中文课程站点**，包含 18 节课、18 张速查卡，侧边栏目录可随时收起/展开，课程页底部有「上一节 / 下一节」导航：

```text
learn/index.html   ← 从这里开始（课程首页 / 目录树）
```

**打开方式（任选其一）：**

```bash
# 方式一：直接用浏览器打开（macOS）
open learn/index.html

# 方式二：在 learn/ 目录下启动一个静态服务（推荐，避免浏览器 file:// 下部分特性受限）
cd learn && python3 -m http.server 8000
# 然后访问 http://localhost:8000/
```

进入后用法：

- 点左上角 **≡** 按钮打开/收起课程目录树；目录状态会被浏览器记住。
- 目录树中每节课标题末尾的小按钮 `卡` 可直接跳到对应速查卡（速查卡里同一位置是 `课`，可跳回课程）。
- 课程页底部有 **← 上一节 / 🏠 / 下一节 →** 导航，支持线性学习。
- 学完一节，按课里的 "动手验证" 部分 `cd demos/NN-xxx && mvn spring-boot:run` 跑对应 Demo，再用给好的 `curl` 命令验证。

---

## 适合人群

适合以下开发者：

- 已掌握 Java 基础。
- 已熟悉 Spring Boot Web 开发。
- 已了解 Maven、REST API、JSON、配置文件等常见后端技术。
- 想学习 Spring AI Alibaba。
- 想从普通 ChatBot 逐步进阶到企业级 AI Agent。
- 想了解 Tool Calling、RAG、Graph、MCP、多 Agent、模型网关等能力。

---

## 当前项目状态

当前仓库已经包含 **全部 19 个阶段** 的 Demo、课程 HTML 与参考卡：

```text
demos/01-environment-check          demos/11-order-agent
demos/02-chatbot                    demos/12-markdown-rag
demos/03-streaming-chatbot          demos/13-pdf-rag
demos/04-prompt-assistant           demos/14-rag-tuning
demos/05-structured-output          demos/15-customer-service-agent
demos/06-prompt-template            demos/16-graph-agent
demos/07-chat-memory                demos/17-mcp-integration
demos/08-learning-advisor           demos/18-observability-eval
demos/09-calculator-tool            demos/19-agent-platform
demos/10-weather-tool

learn/lessons/0001 ~ 0018.html          # 每章的完整课程页
learn/reference/0001 ~ 0018-quick-reference.html   # 复习用速查卡
```

端口分配：`8080` (01) → `8098` (19)，每章递增 1，便于同时启动多个 demo 对比。

### Maven 网络提示

仓库里所有 demo 默认使用 Spring Boot 3.3.5 + Spring AI Alibaba 1.0.0.2；涉及 RAG 的章节（12/13/14/15）同时升级到 Spring AI 1.1.0 以使用 `QuestionAnswerAdvisor`（原 `org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor` 在 1.1 中移至 `org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor`）。

所有 19 个 demo 已通过 `mvn compile` 验证。如果你本机的 `~/.m2/settings.xml` 配置了无法连通的内网 mirror，
请自行修复或临时指向公共镜像（阿里云、Maven Central 等），再执行 `mvn spring-boot:run`。

### 章节 → Demo → 课程/参考 索引

| 阶段 | Demo | 端口 | 课程 | 参考卡 |
| --- | --- | --- | --- | --- |
| 01 | `01-environment-check` | 8080 | `lessons/0001-…` | `reference/0001-…` |
| 02 | `02-chatbot` | 8081 | `lessons/0001-…` | `reference/0001-…` |
| 03 | `03-streaming-chatbot` | 8082 | `lessons/0002-…` | `reference/0002-…` |
| 04 | `04-prompt-assistant` | 8083 | `lessons/0003-prompt-assistant.html` | `reference/0003-…` |
| 05 | `05-structured-output` | 8084 | `lessons/0004-structured-output.html` | `reference/0004-…` |
| 06 | `06-prompt-template` | 8085 | `lessons/0005-prompt-template.html` | `reference/0005-…` |
| 07 | `07-chat-memory` | 8086 | `lessons/0006-chat-memory.html` | `reference/0006-…` |
| 08 | `08-learning-advisor` | 8087 | `lessons/0007-learning-advisor.html` | `reference/0007-…` |
| 09 | `09-calculator-tool` | 8088 | `lessons/0008-calculator-tool.html` | `reference/0008-…` |
| 10 | `10-weather-tool` | 8089 | `lessons/0009-weather-tool.html` | `reference/0009-…` |
| 11 | `11-order-agent` | 8090 | `lessons/0010-order-agent.html` | `reference/0010-…` |
| 12 | `12-markdown-rag` | 8091 | `lessons/0011-markdown-rag.html` | `reference/0011-…` |
| 13 | `13-pdf-rag` | 8092 | `lessons/0012-pdf-rag.html` | `reference/0012-…` |
| 14 | `14-rag-tuning` | 8093 | `lessons/0013-rag-tuning.html` | `reference/0013-…` |
| 15 | `15-customer-service-agent` | 8094 | `lessons/0014-customer-service-agent.html` | `reference/0014-…` |
| 16 | `16-graph-agent` | 8095 | `lessons/0015-graph-agent.html` | `reference/0015-…` |
| 17 | `17-mcp-integration` | 8096 | `lessons/0016-mcp.html` | `reference/0016-…` |
| 18 | `18-observability-eval` | 8097 | `lessons/0017-observability-eval.html` | `reference/0017-…` |
| 19 | `19-agent-platform` | 8098 | `lessons/0018-agent-platform.html` | `reference/0018-…` |

`01-environment-check` 用于验证 Spring AI Alibaba 开发环境，包含：

- Spring Boot 应用启动。
- Spring AI Alibaba DashScope Starter 接入。
- DashScope / 百炼 API Key 环境变量读取。
- 模型名称配置读取。
- 环境状态检查接口。
- `ChatClient` 最小模型调用验证接口。

`02-chatbot` 用于学习最小 ChatBot 业务接口，包含：

- ChatBot 状态检查接口。
- 基于 `ChatClient` 的同步问答接口。
- `defaultSystem` 默认系统提示词。
- API Key 缺失和问题为空的基础校验。

`03-streaming-chatbot` 用于学习流式 ChatBot 和 SSE，包含：

- ChatBot 状态检查接口。
- 同步问答接口，用于和流式接口对比。
- 基于 `ChatClient.stream().content()` 的流式问答接口。
- `text/event-stream` SSE 输出。
- `curl -N` 流式验证方式。

---

## 项目结构

```text
.
├── README.md
├── demos/                    # 19 个可运行 Demo（01-env-check … 19-agent-platform）
│   └── NN-xxx/
│       ├── pom.xml
│       └── src/main/{java,resources}
├── learn/                    # 课程站点（静态 HTML）
│   ├── index.html            # ★ 课程目录首页（从这里打开）
│   ├── MISSION.md            # 课程使命 / 范围 / 约束
│   ├── NOTES.md              # 教学笔记
│   ├── RESOURCES.md          # 参考资料索引
│   ├── assets/
│   │   ├── course.css        # 课程正文样式
│   │   ├── nav.css           # 侧边栏 + 上下节导航样式
│   │   ├── nav.js            # 侧边栏目录树 + 上/下一节导航脚本
│   │   └── quiz.js           # 课中思考卡片交互
│   ├── lessons/              # 18 节课程正文（0001-… ~ 0018-….html）
│   │   ├── 0001-from-environment-check-to-chatbot.html
│   │   ├── …
│   │   └── 0018-agent-platform.html
│   ├── reference/            # 18 张速查卡（和课程一一对应）
│   │   ├── 0001-chatclient-quick-reference.html
│   │   ├── …
│   │   └── 0018-agent-platform-quick-reference.html
│   └── learning-records/     # 学习记录
└── notes/
    └── ai-agent-learning-roadmap.md
```

> 课程站点内的任意页面都可通过左上角 **≡** 打开目录树跳转到其它课程/速查卡，页面底部有「← 上一节 / 🏠 / 下一节 →」线性导航，**不必手动记文件名**。

---

## 学习路线总览

```text
Spring AI Alibaba 基础认知
  ↓
ChatClient 与 ChatBot
  ↓
Prompt 工程与结构化输出
  ↓
会话记忆 Memory
  ↓
Tool Calling 工具调用
  ↓
RAG 知识库问答
  ↓
业务 Agent 流程设计
  ↓
Graph 工作流编排
  ↓
MCP 工具生态集成
  ↓
评测、观测、安全、成本控制
  ↓
企业级 AI Agent 架构设计
```

详细学习规划见：

```text
notes/ai-agent-learning-roadmap.md
```

---

## 推荐学习周期

| 路线 | 学习强度 | 总时间 | 达成水平 |
| --- | --- | --- | --- |
| 快速入门 | 每天 1～2 小时 | 6～8 周 | 能开发 ChatBot、基础 Tool Calling Agent 和简单 RAG Demo |
| 系统进阶 | 每天 2～3 小时 | 3～5 个月 | 能开发企业知识库助手、智能客服 Agent、业务工具型 Agent |
| 架构成长 | 持续项目实践 | 6～9 个月 | 能设计企业级 AI Agent 应用架构 |
| 资深架构 | 长期沉淀 | 9～12 个月以上 | 能设计 Agent 平台、模型网关、评测体系和知识库治理体系 |

---

## 阶段化 Demo 规划

| 阶段 | Demo 项目 | 主要目的 | 建议时间 |
| --- | --- | --- | --- |
| 1 | Spring AI Alibaba 环境检查 Demo | 理解框架、依赖和模型配置 | 3～5 天 |
| 2 | 最小 ChatBot Demo | 跑通模型调用闭环 | 3～5 天 |
| 3 | 流式 ChatBot Demo | 掌握 SSE 流式输出 | 2～3 天 |
| 4 | Prompt 学习助手 Demo | 掌握 Prompt 控制模型回答 | 4～5 天 |
| 5 | 结构化 JSON 输出 Demo | 掌握可解析输出 | 4～5 天 |
| 6 | Prompt 模板管理 Demo | 建立 Prompt 工程化意识 | 3～5 天 |
| 7 | 多轮对话 Demo | 掌握 Memory 和会话隔离 | 1 周 |
| 8 | 学习顾问记忆 Demo | 掌握个性化上下文 | 1 周 |
| 9 | 计算器工具 Demo | 入门 Tool Calling | 3～5 天 |
| 10 | 天气查询工具 Demo | 接入外部 API | 1 周 |
| 11 | 订单查询 Agent Demo | 结合业务工具和权限校验 | 1～2 周 |
| 12 | Markdown 知识库助手 Demo | 入门 RAG | 2 周 |
| 13 | PDF 知识库助手 Demo | 处理真实文档 | 2 周 |
| 14 | 知识库参数调优 Demo | 提升 RAG 检索和回答效果 | 1 周 |
| 15 | 智能客服 Agent Demo | 综合 RAG + Tool Calling + Memory | 3～4 周 |
| 16 | 客服 Graph Agent Demo | 掌握确定性流程编排 | 3～4 周 |
| 17 | MCP 工具集成 Demo | 接入外部工具生态 | 3～4 周 |
| 18 | Agent 日志与评测 Demo | 补齐生产化能力 | 3～4 周 |
| 19 | 企业级 Agent 平台原型 | 形成架构设计能力 | 8～12 周 |

---

## 当前已完成 Demo

### 01-environment-check

### 功能说明

`demos/01-environment-check` 是阶段一 Demo，用于学习 Spring AI Alibaba 项目的环境准备和最小模型调用验证。

它包含两个接口：

```http
GET /env/status
GET /env/ping
```

### 技术点

- Spring Boot 3.3.5
- Java 17
- Spring AI Alibaba 1.0.0.2
- DashScope / 百炼模型接入
- `ChatClient`
- 环境变量配置
- API Key 脱敏展示
- 最小模型调用验证

### 运行方式

进入 Demo 目录：

```bash
cd demos/01-environment-check
```

配置 API Key 和模型名：

```bash
export DASHSCOPE_API_KEY=你的 API Key
export DASHSCOPE_CHAT_MODEL=qwen3.7-max
```

启动应用：

```bash
mvn spring-boot:run
```

检查环境配置：

```bash
curl 'http://localhost:8080/env/status'
```

验证模型调用：

```bash
curl 'http://localhost:8080/env/ping'
```

更详细的操作和学习步骤见：

```text
demos/01-environment-check/README.md
```

### 02-chatbot

`demos/02-chatbot` 是阶段二 Demo，用于学习最小 ChatBot 业务接口。

它包含两个接口：

```http
GET /chat/status
POST /chat/ask
```

运行方式：

```bash
cd demos/02-chatbot
export DASHSCOPE_API_KEY=你的 API Key
mvn spring-boot:run
```

测试同步问答：

```bash
curl -X POST 'http://localhost:8081/chat/ask' \
  -H 'Content-Type: application/json' \
  -d '{"question":"请用三点说明 Spring AI Alibaba 在企业开发中的价值"}'
```

### 03-streaming-chatbot

`demos/03-streaming-chatbot` 是阶段三 Demo，用于学习流式 ChatBot 和 SSE。

它包含三个接口：

```http
GET /chat/status
POST /chat/ask
POST /chat/stream
```

运行方式：

```bash
cd demos/03-streaming-chatbot
export DASHSCOPE_API_KEY=你的 API Key
mvn spring-boot:run
```

测试流式问答：

```bash
curl -N -X POST 'http://localhost:8082/chat/stream' \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  -d '{"question":"请用三点解释 SSE 为什么适合 ChatBot"}'
```

---

## 月度学习节奏建议

### 第 1 个月

重点：Spring AI Alibaba 基础、ChatBot、流式输出、Prompt、结构化输出。

建议完成：

- Spring AI Alibaba 环境检查 Demo。
- 最小 ChatBot Demo。
- 流式 ChatBot Demo。
- 学习助手 Prompt Demo。
- 结构化 JSON 输出 Demo。

### 第 2 个月

重点：Memory、Tool Calling、基础业务 Agent。

建议完成：

- 多轮对话 Demo。
- 学习顾问记忆 Demo。
- 计算器工具 Demo。
- 天气工具 Demo。
- 订单查询 Agent。

### 第 3～4 个月

重点：RAG 知识库、业务 Agent、Graph 编排。

建议完成：

- Markdown 知识库助手。
- PDF 知识库助手。
- RAG 参数调优 Demo。
- 智能客服 Agent。
- Graph 客服 Agent。

### 第 5～6 个月

重点：MCP、工程化、评测、观测、安全和成本控制。

建议完成：

- MCP 文件工具 Demo。
- MCP 数据库工具 Demo。
- Agent 日志观测 Demo。
- Prompt 评测 Demo。
- 安全与成本控制 Demo。

### 第 7～12 个月

重点：平台化、架构设计、多 Agent、模型网关、知识库治理。

建议完成：

- 企业知识助手原型。
- 智能客服 Agent 平台原型。
- 多 Agent 协作 Demo。
- 模型网关与评测平台 Demo。

---

## 参考资料

- Spring AI Alibaba 官方文档：<https://java2ai.com/docs/1.0.0.2/overview/>
- ChatBot 入门文档：<https://java2ai.com/docs/1.0.0.2/get-started/chatbot/>
- Spring AI Alibaba 源码仓库：<https://github.com/alibaba/spring-ai-alibaba>
- Spring AI Alibaba Examples：<https://github.com/spring-ai-alibaba/examples>

---

## 学习原则

1. 直接围绕 Spring AI Alibaba 做 AI 应用闭环，不再重复 Java 基础。
2. 先跑通环境检查和 ChatClient，再逐步叠加 Prompt、Memory、Tool Calling、RAG、Graph、MCP。
3. 每个阶段都要有 Demo，不只看文档。
4. Prompt、Memory、Tool Calling、RAG 不要一开始全部混在一起，应该逐步组合。
5. Demo 阶段重功能闭环，生产阶段重安全、观测、评测、成本和权限。
6. Agent 架构设计的核心不是让模型自由发挥，而是让模型在可控流程中完成合适的任务。
7. 企业级 Agent 必须重点关注权限、审计、数据安全、人工确认和回归评测。
