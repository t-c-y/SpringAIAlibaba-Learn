# 12 Markdown 知识库助手 Demo (RAG 入门)

阶段十二目标：跑通 **Ingest → Retrieve → Generate** 的完整 RAG 链路。

## 一、架构

```text
resources/kb/*.md
        │  TextReader
        ▼
List<Document>
        │  TokenTextSplitter (400 tokens, 200 overlap)
        ▼
List<Document> chunks
        │  EmbeddingModel (text-embedding-v2)
        ▼
SimpleVectorStore  ◄───── QuestionAnswerAdvisor ─────► ChatClient
                          topK=4, threshold=0.3
```

**为什么选 SimpleVectorStore？** 只依赖 EmbeddingModel，无需 Milvus / PGVector。适合课堂离线学习，重启后重新灌库。

## 二、知识库内容

`resources/kb/` 下 4 篇 Markdown：Spring AI Alibaba 概览、ChatClient 基础、Tool Calling、RAG 基础。

## 三、接口

```http
POST /rag/ask        # 完整 RAG 问答
POST /rag/retrieve   # 只做检索，看命中片段
```

## 四、运行

```bash
cd demos/12-markdown-rag
export DASHSCOPE_API_KEY=你的 API Key
mvn spring-boot:run

# 命中知识库
curl -X POST 'http://localhost:8091/rag/ask' \
  -H 'Content-Type: application/json' \
  -d '{"question":"RAG 的三个核心步骤是什么？"}'

# 未收录
curl -X POST 'http://localhost:8091/rag/ask' \
  -H 'Content-Type: application/json' \
  -d '{"question":"Spring AI Alibaba 支持 GPU 加速吗？"}'

# 检查召回
curl -X POST 'http://localhost:8091/rag/retrieve' \
  -H 'Content-Type: application/json' \
  -d '{"question":"如何注册一个工具？"}'
```

## 五、学习检查点

- [ ] 能画出 Ingest、Retrieve、Generate 三阶段的数据流图。
- [ ] 能解释 chunk / overlap 的取舍：太大丢粒度、太小丢上下文。
- [ ] 能通过 `/rag/retrieve` 定位问题："答错"到底是没命中，还是命中了但模型没读好。
- [ ] 知道 `QuestionAnswerAdvisor` 内部帮你做了什么。

## 六、下一步

- 阶段 13：把知识源换成 PDF，学习 `PagePdfDocumentReader`。
- 阶段 14：系统地调 topK / threshold / chunk size，形成"参数评估表"。
