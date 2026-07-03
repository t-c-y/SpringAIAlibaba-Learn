# 14 RAG 参数调优 Demo

阶段十四目标：把 RAG 调参从"感觉"变成"实验"。

## 一、可调参数

| 参数 | 影响 |
| --- | --- |
| chunkSize | 每个切片的 token 数，影响召回粒度 |
| chunkOverlap | 切片之间的重叠，避免关键句被切断 |
| topK | 每次检索返回条数 |
| similarityThreshold | 相似度过滤，噪声兜底 |

## 二、接口

```http
POST /rag/rebuild?chunk=400&overlap=200   # 为一组 (chunk, overlap) 重新建索引
GET  /rag/profiles                        # 已建立的 profile 列表
POST /rag/retrieve                        # 只检索，观察命中
POST /rag/ask                             # 完整 RAG，观察回答
```

## 三、实验步骤

```bash
cd demos/14-rag-tuning
export DASHSCOPE_API_KEY=你的 API Key
mvn spring-boot:run

# 建立两组参数
curl -X POST 'http://localhost:8093/rag/rebuild?chunk=200&overlap=50'
curl -X POST 'http://localhost:8093/rag/rebuild?chunk=800&overlap=200'

# 检索对比
curl -X POST 'http://localhost:8093/rag/retrieve' \
  -H 'Content-Type: application/json' \
  -d '{"profile":"200-50","question":"chunkSize 该怎么选？","topK":4}'

curl -X POST 'http://localhost:8093/rag/retrieve' \
  -H 'Content-Type: application/json' \
  -d '{"profile":"800-200","question":"chunkSize 该怎么选？","topK":4}'

# 生成对比
curl -X POST 'http://localhost:8093/rag/ask' \
  -H 'Content-Type: application/json' \
  -d '{"profile":"800-200","question":"chunkSize 该怎么选？","topK":4,"similarityThreshold":0.3}'
```

## 四、评估表建议

| profile | 命中数 | 命中相关度 | 回答质量 | Prompt token 估算 | 备注 |
| --- | --- | --- | --- | --- | --- |
| 200-50 |  |  |  |  |  |
| 400-100 |  |  |  |  |  |
| 800-200 |  |  |  |  |  |

自己列一份 5～10 条的"标准问题 + 标准答案"，逐一填表。生产上建议做成自动化评测，见后续观测/评测课程。

## 五、学习检查点

- [ ] 能画出四个参数对召回和生成的影响。
- [ ] 能用 `/rag/retrieve` 判断"答错"是检索问题还是生成问题。
- [ ] 明白参数没有绝对最优，只有"在你的数据上更优"。
- [ ] 知道 metadata 过滤（version / tenant / language）比换向量模型性价比高。
