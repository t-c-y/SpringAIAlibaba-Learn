# 09 计算器工具 Demo (Tool Calling 入门)

阶段九目标：第一次让模型"调用你写的 Java 方法"。

## 一、核心机制

```text
用户 ─► ChatClient ─► LLM
                       │  判断是否需要工具
                       ▼
                     Tool 调用（工具方法在你自己的 JVM 里执行）
                       │
                       ▼
                     结果回填给 LLM
                       │
                       ▼
                     生成最终自然语言回答
```

关键 API：

- `@Tool(description = "...")`：把方法标记为工具，`description` 给模型看，决定它是否选中这个工具。
- `@ToolParam(description = "...")`：告诉模型每个参数是什么含义。
- `ChatClient.Builder.defaultTools(Object...)`：注册工具类实例。

## 二、四个工具方法

`CalculatorTools.java`：
- `add(a, b)` 加
- `subtract(a, b)` 减
- `multiply(a, b)` 乘
- `divide(a, b)` 除，除数为 0 会抛异常

## 三、运行

```bash
cd demos/09-calculator-tool
export DASHSCOPE_API_KEY=你的 API Key
mvn spring-boot:run

curl -X POST 'http://localhost:8088/calc/ask' \
  -H 'Content-Type: application/json' \
  -d '{"question":"帮我计算 (18 + 27) * 3 / 5 是多少？"}'
```

模型典型执行流程：
1. 调用 `add(18, 27)` → 45
2. 调用 `multiply(45, 3)` → 135
3. 调用 `divide(135, 5)` → 27
4. 输出："结果：27"

## 四、学习检查点

- [ ] 能说清楚为什么 `@Tool` 的 `description` 决定模型选不选这个工具。
- [ ] 能解释"一个 Tool 只做一件事"这条工程约束背后的原因（参数命名、Schema、可测试性）。
- [ ] 能通过日志验证模型确实是逐步调用了工具，而不是自己算的（打开 DEBUG 日志能看到 tool call）。
- [ ] 能理解 `temperature=0.1` 在工具调用场景的用途（避免模型跳过工具直接口算）。

## 五、常见踩坑

- 工具 description 写得太模糊，模型不选它 → description 要"面向调用者"，写清"什么时候用"。
- 工具方法抛异常 → Spring AI 会把异常转成结果字符串给模型，模型会做二次决策。
- 参数类型选择 `Object` / `String` → 模型可能传错格式，尽量用具体类型。
