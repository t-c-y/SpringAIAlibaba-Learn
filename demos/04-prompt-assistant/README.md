# 04 Prompt 学习助手 Demo

阶段四目标：从"能问答"升级到"能按你要的格式稳定回答"。

## 一、阶段目标

完成后你能掌握：

1. `defaultSystem()` 的三段结构：**角色 + 输出结构 + 边界**。
2. 业务字段（topic / level / audience）该放在 `user()` 消息里，不是 System Prompt 里。
3. 用 `temperature` 控制回答的稳定性。
4. 通过对比 `/prompt/ask` 和 `/prompt/ask/raw`，直观感受 Prompt 的价值。

## 二、接口

```http
GET  /prompt/status
POST /prompt/ask          # 带 System Prompt
POST /prompt/ask/raw      # 不带 System Prompt，用于对比
POST /prompt/ask/lesson   # 面向课程生成的业务化 Prompt
```

## 三、运行

```bash
cd demos/04-prompt-assistant
export DASHSCOPE_API_KEY=你的 API Key
mvn spring-boot:run
```

## 四、验证

```bash
# 1. 状态
curl 'http://localhost:8083/prompt/status'

# 2. 学习助手回答（会按四段结构输出）
curl -X POST 'http://localhost:8083/prompt/ask' \
  -H 'Content-Type: application/json' \
  -d '{"question":"Spring AI 的 ChatClient 和原生 OpenAI SDK 有什么区别？"}'

# 3. 不带 System Prompt 的裸回答（风格明显更松散）
curl -X POST 'http://localhost:8083/prompt/ask/raw' \
  -H 'Content-Type: application/json' \
  -d '{"question":"Spring AI 的 ChatClient 和原生 OpenAI SDK 有什么区别？"}'

# 4. 课程生成
curl -X POST 'http://localhost:8083/prompt/ask/lesson' \
  -H 'Content-Type: application/json' \
  -d '{"topic":"Spring AI 的 Advisor 机制","level":"进阶","audience":"三年 Java 经验"}'
```

## 五、学习检查点

- [ ] 能背出 System Prompt 的三段：角色、输出结构、边界。
- [ ] 能解释业务变量为什么不该放进 System Prompt。
- [ ] 能说清楚为什么本阶段把 `temperature` 调到 0.3。
- [ ] 能用一句话说清楚 Prompt 工程解决什么问题（提示：不是让模型更聪明，而是让它更稳定）。

## 六、边界

本阶段仍不引入结构化 JSON、Memory 和 Tool。这三样各自单独一课。
