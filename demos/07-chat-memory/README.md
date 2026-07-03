# 07 多轮对话 Demo

阶段七目标：给 ChatBot 装上"记忆"，掌握 `conversationId` 会话隔离。

## 一、核心组件

| 组件 | 作用 |
| --- | --- |
| `ChatMemory` | 会话历史存储抽象 |
| `InMemoryChatMemory` | 默认实现，进程内 Map，重启丢失 |
| `MessageChatMemoryAdvisor` | 调用前拼历史消息、调用后写回历史 |
| `CHAT_MEMORY_CONVERSATION_ID_KEY` | 告诉 Advisor 这条请求属于哪个会话 |
| `CHAT_MEMORY_RETRIEVE_SIZE_KEY` | 每轮最多取回多少条历史消息 |

## 二、接口

```http
POST /chat/ask       # 带 conversationId 的多轮问答
GET  /chat/history   # 查看会话已保存的消息
POST /chat/reset     # 清空会话
```

## 三、运行

```bash
cd demos/07-chat-memory
export DASHSCOPE_API_KEY=你的 API Key
mvn spring-boot:run
```

## 四、验证多轮记忆

```bash
# 会话 A - 第一轮：先介绍自己
curl -X POST 'http://localhost:8086/chat/ask' \
  -H 'Content-Type: application/json' \
  -d '{"conversationId":"userA","question":"我叫小张，是一名有 3 年经验的 Java 开发。"}'

# 会话 A - 第二轮：问自己的名字
curl -X POST 'http://localhost:8086/chat/ask' \
  -H 'Content-Type: application/json' \
  -d '{"conversationId":"userA","question":"我叫什么？请给我一个进阶学习建议。"}'

# 会话 B - 第一轮：应当完全不知道小张
curl -X POST 'http://localhost:8086/chat/ask' \
  -H 'Content-Type: application/json' \
  -d '{"conversationId":"userB","question":"我叫什么？"}'

# 查看历史
curl 'http://localhost:8086/chat/history?conversationId=userA'

# 清空
curl -X POST 'http://localhost:8086/chat/reset?conversationId=userA'
```

## 五、学习检查点

- [ ] 能说清楚 `MessageChatMemoryAdvisor` 在请求前后分别做了什么。
- [ ] 能解释为什么必须传 `conversationId`，不传会怎样（默认合到同一个 default 会话，用户之间串数据）。
- [ ] 知道 `InMemoryChatMemory` 只能用于教学，生产要换成 Redis / DB 版本。
- [ ] 能通过 `/chat/history` 验证"记忆确实存在"。

## 六、生产扩展方向

- Redis 实现 `ChatMemory` 保证多实例共享和持久化。
- 增加消息数上限 / token 上限，避免上下文爆炸。
- 加入摘要（summarize）机制，让长对话不至于把 token 花光。
- `conversationId` 应当由服务端从"用户 + 会话槽"生成，而不是让前端随便传。
