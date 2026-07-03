# 06 Prompt 模板管理 Demo

阶段六目标：把 Prompt 从"硬编码的字符串"升级为"资源目录下的模板文件 + 变量渲染"。

## 一、核心工具

| API | 用途 |
| --- | --- |
| `PromptTemplate(text).render(Map)` | 字符串模板渲染 |
| `SystemPromptTemplate(text).createMessage(Map)` | 生成一条 SystemMessage |
| `new Prompt(List<Message>)` | 手动组装多消息 Prompt |
| `classpath:prompts/*.st` | 把 Prompt 与 Java 代码解耦 |

`.st` 是 [StringTemplate](https://www.stringtemplate.org/) 语法，Spring AI 默认支持。变量占位符用 `{name}`。

## 二、目录结构

```text
src/main/resources/prompts/
├── lesson-assistant-system.st   # System Prompt 模板
└── lesson-user.st               # User Prompt 模板
```

## 三、接口

```http
POST /tmpl/lesson    # 从文件模板生成课程内容
POST /tmpl/simple    # 用一行字符串模板做变量替换
```

## 四、运行

```bash
cd demos/06-prompt-template
export DASHSCOPE_API_KEY=你的 API Key
mvn spring-boot:run

curl -X POST 'http://localhost:8085/tmpl/lesson' \
  -H 'Content-Type: application/json' \
  -d '{"topic":"Spring AI ChatClient","level":"入门","audience":"三年 Java 经验","duration":"30 分钟"}'
```

## 五、学习检查点

- [ ] 能说清楚为什么 Prompt 要模板化（可维护、可 diff、可国际化、非工程师可以改）。
- [ ] 能新增一个 `.st` 文件并加变量，跑通接口。
- [ ] 能解释 `PromptTemplate.render()` 和 `SystemPromptTemplate.createMessage()` 的区别。

## 六、边界

只学 Prompt 模板化。多轮记忆和 Tool Calling 从下一课开始。
