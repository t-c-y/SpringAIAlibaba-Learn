# 08 学习顾问 Memory Demo

阶段八目标：在会话记忆基础上叠加"用户画像"这一层长期记忆，形成两级记忆。

## 一、两层记忆的分工

| 层次 | 存储 | 隔离字段 | 内容 |
| --- | --- | --- | --- |
| 短期 / 会话记忆 | `ChatMemory` | `conversationId` | 一次会话内的对话历史 |
| 长期 / 用户画像 | `LearnerProfileStore` | `userId` | 姓名、目标岗位、已掌握技能、正在学习方向、每周投入时间 |

设计原则：
- 短期记忆放"这次会话说过的话"，会随时间失效或被压缩。
- 长期记忆放"稳定的用户属性"，会跨会话一直生效。
- 长期记忆不是每轮塞进 user 消息，而是渲染进 System Prompt。

## 二、接口

```http
POST   /advisor/profile   # 更新画像
GET    /advisor/profile?userId=U   # 查看画像
DELETE /advisor/profile?userId=U   # 清空画像
POST   /advisor/ask       # 咨询
```

## 三、跑一遍

```bash
cd demos/08-learning-advisor
export DASHSCOPE_API_KEY=你的 API Key
mvn spring-boot:run

# 1. 建立画像
curl -X POST 'http://localhost:8087/advisor/profile' \
  -H 'Content-Type: application/json' \
  -d '{"userId":"u001","name":"小张","targetRole":"AI 应用架构师","mastered":["Spring Boot","MyBatis"],"learning":["Spring AI Alibaba","RAG"],"weeklyBudgetHours":"8h"}'

# 2. 提问 —— 观察建议是否基于画像
curl -X POST 'http://localhost:8087/advisor/ask' \
  -H 'Content-Type: application/json' \
  -d '{"userId":"u001","conversationId":"u001-week1","question":"下周我该重点学什么？"}'

# 3. 追问同一会话
curl -X POST 'http://localhost:8087/advisor/ask' \
  -H 'Content-Type: application/json' \
  -d '{"userId":"u001","conversationId":"u001-week1","question":"再具体一点，每天学多久？"}'
```

## 四、学习检查点

- [ ] 能说清楚为什么"姓名 / 目标岗位"放长期记忆，"上一句话说了什么"放短期记忆。
- [ ] 能理解 System Prompt 里嵌入画像片段的做法，为什么比"每次 user 消息重复带一遍"更省 token。
- [ ] 会用 `/advisor/profile` 更新画像并观察回答变化。

## 五、生产扩展方向

- 用 DB / Redis 持久化画像，版本化管理。
- 长期记忆演化：从"字段化画像"进阶到"知识条目 + 向量检索"（第 12～14 课的 RAG 就是这个方向）。
- 加入"用户确认"流程，避免模型自作主张改画像。
