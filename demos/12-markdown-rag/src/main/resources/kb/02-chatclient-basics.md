# ChatClient 基础

ChatClient 是 Spring AI Alibaba 里业务代码调用大模型的主要入口。你通过 `ChatClient.Builder` 拿到一个可复用的 ChatClient 实例。

同步调用：

```java
String answer = chatClient.prompt()
        .user("请解释 Spring AI Alibaba")
        .call()
        .content();
```

流式调用：

```java
Flux<String> stream = chatClient.prompt()
        .user("请写一段自我介绍")
        .stream()
        .content();
```

`defaultSystem(...)` 用于设置 System Prompt，`defaultAdvisors(...)` 用于挂 Advisor（例如 ChatMemory、QuestionAnswerAdvisor 等）。生产环境建议一个业务场景一个 ChatClient Bean，避免 System Prompt 相互污染。
