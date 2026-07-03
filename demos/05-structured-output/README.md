# 05 结构化 JSON 输出 Demo

阶段五目标：让模型输出可以被 Java 直接反序列化的对象或集合，避免"手动 parse LLM 文本"。

## 一、核心 API

```java
// 单对象
LessonOutline outline = chatClient.prompt()
        .user("请为《Spring AI Alibaba》生成一份课程大纲。")
        .call()
        .entity(LessonOutline.class);

// 集合
List<Checkpoint> list = chatClient.prompt()
        .user("请生成 5 条学习检查点")
        .call()
        .entity(new ParameterizedTypeReference<List<Checkpoint>>() {});
```

`.entity(Type)` 做了三件事：
1. 根据类型生成 JSON Schema 描述。
2. 自动拼接到 Prompt 里，告诉模型必须输出对应结构。
3. 用 `BeanOutputConverter` 把模型返回的文本反序列化成 Java 对象。

## 二、接口

```http
POST /struct/lesson       # 返回单个 LessonOutline
POST /struct/checkpoints  # 返回 List<Checkpoint>
POST /struct/raw          # 反例：不用 entity()，只能拿到 String
```

## 三、运行

```bash
cd demos/05-structured-output
export DASHSCOPE_API_KEY=你的 API Key
mvn spring-boot:run

curl -X POST 'http://localhost:8084/struct/lesson' \
  -H 'Content-Type: application/json' \
  -d '{"topic":"Spring AI Alibaba 基础"}'

curl -X POST 'http://localhost:8084/struct/checkpoints' \
  -H 'Content-Type: application/json' \
  -d '{"topic":"Spring AI Alibaba 基础"}'
```

## 四、学习检查点

- [ ] 能说清楚 `.entity()` 内部帮你做了哪三件事。
- [ ] 知道为什么结构化输出要把 `temperature` 调到 0.1 以下。
- [ ] 明白 record 类型的字段名会成为 JSON 字段名。
- [ ] 能解释：如果模型返回的 JSON 字段不对，`.entity()` 会怎么表现（会抛异常，需要外层重试）。

## 五、边界

只学结构化输出。多轮对话、Tool Calling、RAG 都不引入。
