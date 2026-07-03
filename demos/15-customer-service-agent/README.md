# 15 智能客服 Agent Demo

阶段十五目标：把 **Memory + RAG + Tool** 首次组合到一个 Agent 上。

## 一、三件能力如何合并

```text
用户消息 ──► MessageChatMemoryAdvisor（拼历史消息）
             │
             ▼
        QuestionAnswerAdvisor（检索知识库 topK，拼进 context）
             │
             ▼
        LLM 决策 → 也许调用 Tool（订单/退款）
             │
             ▼
        最终回答 → 记忆写回
```

## 二、内置数据

- 订单：`O-1001` / `O-1002`（属于 u001）、`O-2001`（属于 u002）。
- 知识库：`refund-policy.md`、`shipping-policy.md`、`warranty.md`。

## 三、运行

```bash
cd demos/15-customer-service-agent
export DASHSCOPE_API_KEY=你的 API Key
mvn spring-boot:run

# u001 查询自己的订单（走 Tool）
curl -X POST 'http://localhost:8094/cs/ask' \
  -H 'Content-Type: application/json' -H 'X-User-Id: u001' \
  -d '{"conversationId":"c-001","question":"帮我看看我的订单。"}'

# u001 追问：符合退款政策吗（同时用 Tool + RAG）
curl -X POST 'http://localhost:8094/cs/ask' \
  -H 'Content-Type: application/json' -H 'X-User-Id: u001' \
  -d '{"conversationId":"c-001","question":"O-1002 已经签收 3 天，我想退，符合政策吗？"}'

# u001 申请退款（走 Tool + 明确“待人工确认”）
curl -X POST 'http://localhost:8094/cs/ask' \
  -H 'Content-Type: application/json' -H 'X-User-Id: u001' \
  -d '{"conversationId":"c-001","question":"帮我给 O-1002 提交退款，理由：耳机不适用。"}'

# u001 越权查 u002 的订单
curl -X POST 'http://localhost:8094/cs/ask' \
  -H 'Content-Type: application/json' -H 'X-User-Id: u001' \
  -d '{"conversationId":"c-001","question":"帮我查一下 O-2001 的状态。"}'
```

## 四、学习检查点

- [ ] 能画出 Memory → RAG → Tool → Memory 写回的完整数据流。
- [ ] 明白同一个 ChatClient 内 Advisor 的注册顺序对最终 Prompt 的影响。
- [ ] 能解释为什么退款走"pending + 人工确认"而不是直接执行。
- [ ] 理解为什么把 `currentUserId` 通过 System 追加在 user 消息里，比让前端传更安全。

## 五、生产扩展方向

- 把 InMemoryChatMemory 换成 Redis，多实例共享会话。
- 把 SimpleVectorStore 换成 PGVector / Milvus。
- 退款走真实的"申请→审核→打款"三段流水线，加审计。
- 引入 Sentinel / Nacos 做流控和配置管理。
