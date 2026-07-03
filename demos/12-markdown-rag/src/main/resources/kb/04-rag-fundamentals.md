# RAG 基础

RAG（Retrieval-Augmented Generation，检索增强生成）指的是：先根据用户问题去知识库中检索最相关的片段，再把这些片段拼进 Prompt，让模型基于它们回答。

RAG 的三个核心步骤：

1. Ingest（灌库）：读取文档 → 切片 → 生成向量 → 写入 VectorStore。
2. Retrieve（检索）：把问题也变成向量，做相似度检索，拿回 topK 相关片段。
3. Generate（生成）：把检索片段拼进 System Prompt，让模型基于 context 回答。

Spring AI 提供的关键抽象：

- `DocumentReader`：读文件，例如 `TextReader`、`PagePdfDocumentReader`。
- `TokenTextSplitter`：按 token 数量切片，chunk 之间可以有 overlap。
- `EmbeddingModel`：把文本转成向量。
- `VectorStore`：向量数据库抽象，`SimpleVectorStore` 是内存实现。
- `QuestionAnswerAdvisor`：把检索结果自动拼进 Prompt 的 Advisor。

常见调参项：`topK`、`similarityThreshold`、chunk 大小、chunk overlap。参数没有绝对最优，需要在自己的数据上评估。
