# 11 订单查询 Agent Demo (Tool + 权限)

阶段十一目标：把 Tool Calling 落到真实业务，加入**权限守卫**这一必须的生产要素。

## 一、生产 Agent 的三条边界

1. 用户身份**不允许模型自己填**，必须来自服务端会话上下文。
2. 工具方法内部**必须做权限校验**，不能只靠 Prompt。
3. 写操作要走**人工确认 + 幂等 API**，本课先只演示只读查询。

## 二、机制

```text
HTTP Header X-User-Id
        │
        ▼
OrderAgentController#ask
        │  UserContext.set(userId)
        ▼
ChatClient ──► LLM
                 │  决定调用 listMyOrders / getOrderById
                 ▼
OrderTools 工具方法
        │  从 UserContext.get() 取当前用户
        │  越权访问 → 返回 allowed=false
        ▼
结果回给 LLM，生成自然语言回答
```

## 三、内置数据

| userId | 订单 |
| --- | --- |
| u001 | O-1001（MacBook Pro）、O-1002（AirPods Pro） |
| u002 | O-2001（iPad Air） |

## 四、运行

```bash
cd demos/11-order-agent
export DASHSCOPE_API_KEY=你的 API Key
mvn spring-boot:run

# u001 查看自己的订单
curl -X POST 'http://localhost:8090/order/ask' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: u001' \
  -d '{"question":"我有哪些订单？"}'

# u001 查看具体订单
curl -X POST 'http://localhost:8090/order/ask' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: u001' \
  -d '{"question":"O-1001 的状态是什么？"}'

# u001 越权尝试查 u002 的 O-2001
curl -X POST 'http://localhost:8090/order/ask' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: u001' \
  -d '{"question":"查一下 O-2001 的详情"}'
```

## 五、学习检查点

- [ ] 能解释为什么 userId 一定不从模型参数拿。
- [ ] 明白工具方法里返回 `allowed=false` 是"业务上的权限"，异常是"技术上的错误"，两者不要混。
- [ ] 能说清楚 ThreadLocal 在这里的作用，以及为什么必须在 `finally` 里 `clear`。
- [ ] 能想到写操作场景的额外要求：人工确认、幂等 key、审计日志。
