package com.example.springaialibaba.markdownrag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Markdown 知识库助手 Controller。
 *
 * 教学要点：
 * 1. QuestionAnswerAdvisor 是 Spring AI 官方 RAG Advisor，做了三件事：
 *    - 用问题去 VectorStore 检索 topK 片段。
 *    - 把片段拼进 System Prompt（作为 context）。
 *    - 让模型基于 context 回答。
 * 2. SearchRequest 控制 topK、相似度阈值、metadata 过滤等参数。
 * 3. /rag/retrieve 单独暴露检索结果，方便你和最终回答对照，判断是"检索问题"还是"生成问题"。
 */
@RestController
public class MarkdownRagController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final String apiKey;

    public MarkdownRagController(ChatClient.Builder builder,
                                 VectorStore vectorStore,
                                 @Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        this.vectorStore = vectorStore;
        this.apiKey = apiKey;

        this.chatClient = builder
                .defaultSystem("""
                        你是一名 Spring AI Alibaba 知识库助手。
                        只能基于提供的 context 回答问题：
                        1. 如果 context 里没有信息，请回答“抱歉，知识库暂未收录相关内容”。
                        2. 回答末尾用【参考来源】列出用到的 source 文件名。
                        3. 不要编造 API 名称或版本号。
                        """)
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder().topK(4).similarityThreshold(0.3d).build())
                        .build())
                .build();
    }

    @PostMapping("/rag/ask")
    public Object ask(@RequestBody AskRequest req) {
        if (apiKey == null || apiKey.isBlank()) return Map.of("success", false, "error", "DASHSCOPE_API_KEY is not configured");
        if (req == null || req.question() == null || req.question().isBlank())
            return Map.of("success", false, "error", "question must not be blank");
        try {
            String answer = chatClient.prompt().user(req.question()).call().content();
            return Map.of("success", true, "question", req.question(), "answer", answer);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** 仅返回检索到的片段，用于排查召回效果。 */
    @PostMapping("/rag/retrieve")
    public Object retrieve(@RequestBody AskRequest req) {
        var results = vectorStore.similaritySearch(SearchRequest.builder()
                .query(req.question()).topK(4).similarityThreshold(0.0d).build());
        return Map.of("question", req.question(), "hits",
                results.stream().map(d -> Map.of(
                        "source", d.getMetadata().getOrDefault("source", "unknown"),
                        "score", d.getMetadata().getOrDefault("distance", "n/a"),
                        "text", truncate(d.getText(), 240)
                )).toList());
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    public record AskRequest(String question) {}
}
