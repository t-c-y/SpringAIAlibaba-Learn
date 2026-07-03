# 18 Agent 日志与评测 Demo

阶段十八目标：把 Agent 从"能跑"变成"可观察 + 可评估"。这是生产落地的两条必修课。

## 一、可观察

每次调用记录：`question / answer / latencyMs / estTokens / userId / timestamp`。真实场景要把这些接入 ELK / Loki 或 ClickHouse，并配合 OpenTelemetry 输出 traces / metrics。

## 二、评测：LLM-as-a-judge

数据集式评测：准备一组"标准问题 + 标准答案"，让被评模型回答，再让一个"评审模型"判定语义是否等价。

**注意**：LLM 评审不是万能的，适合语义等价类问题；数值精确、事实类问题需要引入规则或人工。

## 三、接口

```http
POST /obs/ask     # 记录并回答
GET  /obs/logs    # 查看最近日志
POST /obs/eval    # 批量评测
```

## 四、运行

```bash
cd demos/18-observability-eval
export DASHSCOPE_API_KEY=你的 API Key
mvn spring-boot:run

curl -X POST 'http://localhost:8097/obs/ask' -H 'Content-Type: application/json' \
  -d '{"userId":"u001","question":"ChatClient 有几种调用方式？"}'

curl 'http://localhost:8097/obs/logs?n=5'

curl -X POST 'http://localhost:8097/obs/eval' -H 'Content-Type: application/json' \
  -d '{
    "cases":[
      {"question":"ChatClient 有哪两种调用方式？","expected":"同步 .call().content() 与 流式 .stream().content()"},
      {"question":"@Tool 注解的 description 有什么作用？","expected":"作为模型选择工具的依据，写清适用场景"},
      {"question":"RAG 三步骤","expected":"Ingest 灌库 → Retrieve 检索 → Generate 生成"}
    ]
  }'
```

## 五、学习检查点

- [ ] 能画出"业务调用 → Log Store → 观测大盘"数据流。
- [ ] 能说清楚 LLM-as-a-judge 的适用边界。
- [ ] 能设计一份 20 条的评测集，覆盖 happy path、边界、拒答、越权。
- [ ] 会读取 `estTokens` 做成本上限告警（教学阶段的估算方法只作参考）。

## 六、生产扩展方向

- 打通 OpenTelemetry Traces，把 Advisor / Tool 每一步都串联。
- 用 Prometheus 采集 latency P50/P95/P99，配合 Grafana。
- 定期跑评测集，形成"发布前门禁"。
- 建立 red-team 数据集，专门覆盖越权、幻觉、提示词注入。
