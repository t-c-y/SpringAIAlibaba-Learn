# Tool Calling 基础

Tool Calling 让大模型可以调用你写的 Java 方法。在 Spring AI Alibaba 中，通常用注解声明：

```java
@Tool(description = "两个数相加")
public double add(@ToolParam(description = "第一个加数") double a,
                  @ToolParam(description = "第二个加数") double b) {
    return a + b;
}
```

关键点：

1. `@Tool` 的 description 是模型选择工具的主要依据，一定要写清楚“什么场景下应该用这个工具”。
2. 工具方法不要做副作用，除非能保证幂等；对于写操作，建议加人工确认或幂等 key。
3. 用户身份不应作为工具参数，应从会话上下文（例如 ThreadLocal / RequestScope）读取，避免大模型越权。

工具注册：

```java
ChatClient client = builder
        .defaultTools(new CalculatorTools())
        .build();
```
