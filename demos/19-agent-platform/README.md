# 19 企业级 Agent 平台原型 Demo

阶段十九目标：把前面 18 课的能力**装配**成一个可扩展的 Agent 平台骨架。

## 一、总体架构

```text
HTTP Ingress
    │
    ▼
PlatformController
    │  ① 鉴权 (X-User-Id) → RequestGuard 限流
    │  ② AgentRouter 用轻量模型分类
    │  ▼
Learning / CustomerService / Coding Agent
    │  ③ 调用 ChatClient (可插 Advisor/Tool/RAG)
    │  ▼
统一审计日志 (in-memory)
```

## 二、组件

| 组件 | 教学版 | 生产建议 |
| --- | --- | --- |
| `Agent` 抽象 | Java 接口 | 支持热部署，从注册中心加载 |
| `AgentRouter` | LLM 分类 | 规则 + 向量检索 + LLM，多级 |
| `RequestGuard` | 每分钟 20 QPS 上限 | Sentinel / Resilience4j |
| 审计日志 | in-memory | Kafka + ClickHouse / OTLP |
| 模型网关 | 依赖 DashScope Starter | 抽象出 provider，主备切换、成本路由 |

## 三、接口

```http
GET  /platform/agents   # 列出注册的 Agent
POST /platform/ask      # 统一入口
GET  /platform/audit    # 查审计
```

## 四、运行

```bash
cd demos/19-agent-platform
export DASHSCOPE_API_KEY=你的 API Key
mvn spring-boot:run

curl 'http://localhost:8098/platform/agents'

# 会被 router 分到 coding
curl -X POST 'http://localhost:8098/platform/ask' -H 'Content-Type: application/json' -H 'X-User-Id: u001' \
  -d '{"question":"用 Spring Boot 写一个最小 REST 接口。"}'

# 会被 router 分到 customer-service
curl -X POST 'http://localhost:8098/platform/ask' -H 'Content-Type: application/json' -H 'X-User-Id: u001' \
  -d '{"question":"我的订单还没到怎么办？"}'

# learning
curl -X POST 'http://localhost:8098/platform/ask' -H 'Content-Type: application/json' -H 'X-User-Id: u001' \
  -d '{"question":"Spring AI Alibaba 的 Advisor 是什么？"}'

# 审计
curl 'http://localhost:8098/platform/audit?n=5'
```

## 五、学习检查点

- [ ] 能画出上面的架构图。
- [ ] 能说清楚"路由用便宜模型 + 具体 Agent 用贵模型"的成本设计。
- [ ] 能给 `RequestGuard` 增加"按 Agent 单独配额"的功能。
- [ ] 能列出真正落地时缺什么：安全审计、成本计费、多租户、灰度发布、评测门禁、模型主备切换、内容审核…

## 六、下一步

- 引入 Spring Cloud Gateway 做真实网关。
- 加入 Sentinel 做流控 + 降级。
- 把审计写到 Kafka。
- 用 Nacos 存 System Prompt / 路由规则，配置中心化。
- 建立发布前评测门禁（复用阶段 18 的评测能力）。
