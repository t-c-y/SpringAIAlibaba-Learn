# 16 Graph 客服 Agent Demo

阶段十六目标：把"让 LLM 自由发挥"升级为"显式状态机 + LLM 作为节点"，掌握 Graph 编排思维。

## 一、为什么要 Graph

前 15 课的 Agent 是"一个 ChatClient 挂 Advisor + Tool"的模式，模型有很大自由度。

生产环境常见的问题：
- 关键动作模型不听话（例如 直接执行退款）。
- 意图判断错误，流程走乱，难排查。
- 想插入"人工审批"这样的非模型步骤很别扭。

**Graph 的解法**：把流程画成有限状态机，节点内可以调用 LLM，但**节点间的迁移是代码逻辑**，不是 LLM 自由发挥。

## 二、本课 Graph

```text
        ┌─────────────┐
        │  CLASSIFY   │   LLM 意图分类
        └─────┬───────┘
              │
   ┌──────────┼─────────────┬──────────────┐
   ▼          ▼             ▼              ▼
 ORDER_    POLICY_       REFUND_        SMALL_
 QUERY     QA            CONFIRM        TALK
   │          │             │              │
   ▼          ▼             ▼              ▼
              (统一到 END)
```

关键设计：
- 状态放在 `ChatState`，节点显式读写，可打日志、可回放。
- `REFUND_CONFIRM` 不真的退款，只登记 pending，交给人工审批。
- Runtime 逻辑集中在 `step()`，替换成 Spring AI Alibaba 官方 Graph SDK 时只需换 Runtime。

## 三、运行

```bash
cd demos/16-graph-agent
export DASHSCOPE_API_KEY=你的 API Key
mvn spring-boot:run

curl -X POST 'http://localhost:8095/graph/ask' \
  -H 'Content-Type: application/json' -H 'X-User-Id: u001' \
  -d '{"question":"我的订单都到哪了？"}'

curl -X POST 'http://localhost:8095/graph/ask' \
  -H 'Content-Type: application/json' -H 'X-User-Id: u001' \
  -d '{"question":"iPad 已经签收 10 天，能不能退？"}'

curl -X POST 'http://localhost:8095/graph/ask' \
  -H 'Content-Type: application/json' -H 'X-User-Id: u001' \
  -d '{"question":"帮我给 O-1002 提交退款申请。"}'

curl -X POST 'http://localhost:8095/graph/ask' \
  -H 'Content-Type: application/json' -H 'X-User-Id: u001' \
  -d '{"question":"你好呀 :)"}'
```

响应里的 `trace` 字段就是这次会话经过的节点序列，方便回放。

## 四、学习检查点

- [ ] 能画出本课的状态机图。
- [ ] 能说清楚"LLM 判 intent、代码判 transition"这一分工的价值。
- [ ] 能在 REFUND_CONFIRM 节点后加一段"审批通过后打款"的伪代码。
- [ ] 明白官方 Graph SDK 稳定后可以直接替换 `step()`，业务节点保持不变。
