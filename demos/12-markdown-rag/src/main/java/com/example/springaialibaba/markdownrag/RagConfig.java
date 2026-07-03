package com.example.springaialibaba.markdownrag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * 向量库配置（教学版）。
 *
 * SimpleVectorStore 是 Spring AI 内置的“内存 + 可选磁盘持久化”实现，非常适合入门：
 * - 只依赖 EmbeddingModel。
 * - 不引入 Milvus/Chroma/PGVector 等外部依赖。
 * - 重启后需要重新加载文档，但教学阶段刚好用来观察检索链路。
 *
 * 文档处理链路：
 *   Markdown 资源 → TextReader 读取 → 加 metadata → TokenTextSplitter 切片 → 灌入向量库
 */
@Configuration
public class RagConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public ApplicationRunner loadKnowledgeBase(VectorStore vectorStore) {
        return args -> {
            List<String> mdFiles = List.of(
                    "kb/01-spring-ai-alibaba-overview.md",
                    "kb/02-chatclient-basics.md",
                    "kb/03-tool-calling.md",
                    "kb/04-rag-fundamentals.md"
            );

            List<Document> chunks = new ArrayList<>();
            TokenTextSplitter splitter = new TokenTextSplitter(400, 200, 10, 5000, true);

            for (String path : mdFiles) {
                Resource res = new ClassPathResource(path);
                if (!res.exists()) continue;
                TextReader reader = new TextReader(res);
                reader.getCustomMetadata().put("source", path);
                List<Document> docs = reader.get();
                // 使用 TokenTextSplitter 把长文档切成语义片段，chunk 之间保留 overlap，避免关键信息被切碎。
                chunks.addAll(splitter.apply(docs));
            }

            if (!chunks.isEmpty()) {
                vectorStore.add(chunks);
            }
            System.out.printf("[RAG] loaded %d chunks from %d markdown files%n", chunks.size(), mdFiles.size());
        };
    }
}
