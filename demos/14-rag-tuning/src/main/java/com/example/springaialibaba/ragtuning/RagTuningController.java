package com.example.springaialibaba.ragtuning;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RAG 调参台。
 *
 * 教学目标：给每一组 (chunkSize, overlap) 建一个独立的 VectorStore，
 * 提问时选择一组配置执行检索 + 生成，直观感受参数变化对召回和回答的影响。
 *
 * 建议实验：
 * - chunkSize 大 → 一个 chunk 包含更多上下文，但可能召回不精细；
 * - chunkSize 小 → 召回精细但可能语义碎裂；
 * - overlap 保守值：chunkSize 的 20%~30%。
 * - topK 太大会让 Prompt 变长、成本上升，太小会漏；threshold 用来兜住相似度过低的噪声。
 */
@RestController
public class RagTuningController {

    private final ChatClient.Builder builder;
    private final EmbeddingModel embeddingModel;
    private final String apiKey;

    /** 每组配置一个 VectorStore，key 形如 "400-200"。 */
    private final Map<String, VectorStore> stores = new ConcurrentHashMap<>();

    public RagTuningController(ChatClient.Builder builder,
                               EmbeddingModel embeddingModel,
                               @Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        this.builder = builder;
        this.embeddingModel = embeddingModel;
        this.apiKey = apiKey;
    }

    /**
     * 用不同 (chunkSize, overlap) 灌一次同一份知识库，产出对应 profile。
     * 建议先跑一遍 POST /rag/rebuild?chunk=200&overlap=50 和 chunk=800&overlap=200，然后对比检索质量。
     */
    @PostMapping("/rag/rebuild")
    public Object rebuild(@RequestParam(defaultValue = "400") int chunk,
                          @RequestParam(defaultValue = "200") int overlap) {
        String profile = chunk + "-" + overlap;
        VectorStore vs = SimpleVectorStore.builder(embeddingModel).build();

        List<String> files = List.of(
                "kb/01-spring-ai-alibaba-overview.md",
                "kb/02-rag-tuning-notes.md");

        List<Document> chunks = new ArrayList<>();
        TokenTextSplitter splitter = new TokenTextSplitter(chunk, overlap, 10, 5000, true);
        for (String path : files) {
            var res = new ClassPathResource(path);
            if (!res.exists()) continue;
            TextReader reader = new TextReader(res);
            reader.getCustomMetadata().put("source", path);
            chunks.addAll(splitter.apply(reader.get()));
        }
        vs.add(chunks);
        stores.put(profile, vs);
        return Map.of("profile", profile, "chunkSize", chunk, "overlap", overlap,
                "totalChunks", chunks.size(),
                "hint", "调用 /rag/ask 时传 profile=" + profile + " 使用本次结果。");
    }

    @GetMapping("/rag/profiles")
    public Object profiles() {
        return Map.of("profiles", stores.keySet());
    }

    /**
     * 对某个 profile 执行检索。方便逐 topK / threshold 观察召回。
     */
    @PostMapping("/rag/retrieve")
    public Object retrieve(@RequestBody QueryRequest req) {
        VectorStore vs = pick(req.profile());
        if (vs == null) return Map.of("error", "profile not built yet, call /rag/rebuild first");
        var hits = vs.similaritySearch(SearchRequest.builder()
                .query(req.question())
                .topK(req.topK() == null ? 4 : req.topK())
                .similarityThreshold(req.similarityThreshold() == null ? 0.0d : req.similarityThreshold())
                .build());
        return Map.of("profile", req.profile(), "count", hits.size(),
                "hits", hits.stream().map(d -> Map.of(
                        "source", d.getMetadata().getOrDefault("source", "unknown"),
                        "distance", d.getMetadata().getOrDefault("distance", "n/a"),
                        "text", truncate(d.getText(), 200)
                )).toList());
    }

    /**
     * 对某个 profile 执行完整 RAG：把 topK 片段拼进 Prompt 再问模型。
     * 这里手写拼 Prompt，是为了让你看清 QuestionAnswerAdvisor 内部做的是什么。
     */
    @PostMapping("/rag/ask")
    public Object ask(@RequestBody QueryRequest req) {
        if (apiKey == null || apiKey.isBlank()) return Map.of("error", "DASHSCOPE_API_KEY is not configured");
        VectorStore vs = pick(req.profile());
        if (vs == null) return Map.of("error", "profile not built yet, call /rag/rebuild first");

        var hits = vs.similaritySearch(SearchRequest.builder()
                .query(req.question())
                .topK(req.topK() == null ? 4 : req.topK())
                .similarityThreshold(req.similarityThreshold() == null ? 0.3d : req.similarityThreshold())
                .build());

        String context = String.join("\n\n---\n\n", hits.stream()
                .map(d -> "【source=" + d.getMetadata().getOrDefault("source", "unknown") + "】\n" + d.getText())
                .toList());

        String user = """
                下面是从知识库中检索到的 context：

                %s

                请基于以上 context 回答：%s
                如果 context 不足以回答，请直接说明"context 不足"。回答末尾列出用到的 source。
                """.formatted(context.isBlank() ? "（无命中）" : context, req.question());

        String answer = builder.build().prompt().user(user).call().content();
        return Map.of("profile", req.profile(), "usedHits", hits.size(),
                "question", req.question(), "answer", answer);
    }

    private VectorStore pick(String profile) {
        if (profile == null || profile.isBlank()) return stores.get("400-200");
        return stores.get(profile);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    public record QueryRequest(String profile, String question, Integer topK, Double similarityThreshold) {}
}
