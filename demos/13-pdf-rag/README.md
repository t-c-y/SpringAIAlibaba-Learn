# 13 PDF 知识库助手 Demo

阶段十三目标：把 RAG 的知识源从 Markdown 换成真实 PDF，学会**按页引用**。

## 一、核心组件

| 组件 | 作用 |
| --- | --- |
| `PagePdfDocumentReader` | 逐页读 PDF，metadata 里带 `page_number` |
| `PdfDocumentReaderConfig` | 控制页边距、每页 Document 数 |
| `SimpleVectorStore` | 内存向量库，重启失效 |
| `QuestionAnswerAdvisor` | 自动检索 topK 并拼进 Prompt |

## 二、接口

```http
POST /pdf/upload      # multipart 上传 PDF
POST /pdf/retrieve    # 只做检索，看命中的 page
POST /pdf/ask         # 完整 RAG 问答
```

## 三、运行

> 如果你的 `~/.m2/settings.xml` 配置了无法连通的内网 Nexus mirror（导致无法拉 `spring-ai-bom` / `spring-ai-pdf-document-reader`），
> 请先自行修复 `~/.m2/settings.xml`（去掉不可达的 mirror，或临时切换到阿里云公共镜像 / Maven Central）。

```bash
cd demos/13-pdf-rag
export DASHSCOPE_API_KEY=你的 API Key
mvn spring-boot:run

# 上传你的 PDF（例如一份产品手册）
curl -F 'file=@/path/to/your.pdf' 'http://localhost:8092/pdf/upload'

# 提问
curl -X POST 'http://localhost:8092/pdf/ask' \
  -H 'Content-Type: application/json' \
  -d '{"question":"这份 PDF 里 XX 章节讲了什么？"}'

# 检查命中的页码
curl -X POST 'http://localhost:8092/pdf/retrieve' \
  -H 'Content-Type: application/json' \
  -d '{"question":"关键词"}'
```

> 如果你想让启动时自动灌库，把 PDF 命名为 `sample.pdf` 放到 `src/main/resources/pdf/` 下即可（该目录默认没有文件，仓库不带二进制）。

## 四、学习检查点

- [ ] 能说清楚 `PagePdfDocumentReader` 和 `TextReader` 的差异（PDF 会保留 page metadata）。
- [ ] 会在 System Prompt 里要求模型输出 `[page N]` 引用，实现"可追溯"。
- [ ] 明白扫描版 PDF（纯图片）无法直接抽文字，需要先 OCR，本课不覆盖。
- [ ] 能通过 `/pdf/retrieve` 看命中片段和页码，快速判断问题在检索还是生成。

## 五、生产扩展方向

- 用 `ParagraphPdfDocumentReader` 按段落切分，保留章节结构。
- 换向量库到 PGVector / Milvus，避免重启失库。
- 对同一份 PDF 建立版本，`metadata.version` 支持"只查最新版"。
- 结合 OCR（PaddleOCR / TrOCR），处理扫描件。
