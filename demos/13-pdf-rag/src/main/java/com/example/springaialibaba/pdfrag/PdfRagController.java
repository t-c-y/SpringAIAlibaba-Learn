package com.example.springaialibaba.pdfrag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * PDF 知识库助手。
 *
 * 教学要点：
 * 1. 使用 PagePdfDocumentReader 读取 PDF，每页产生一个 Document，metadata 里带 page_number。
 * 2. 通过 PdfDocumentReaderConfig 控制“是否跳过标题页、页边距、段落间距”等排版参数。
 * 3. 支持通过 /pdf/upload 上传一个新的 PDF 灌库；启动时如果 resources 下有示例 PDF，会自动加载。
 *
 * SimpleVectorStore 依旧是内存实现，重启会失效；教学阶段够用。
 */
@Configuration
@RestController
public class PdfRagController {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final String apiKey;

    public PdfRagController(ChatClient.Builder builder,
                            EmbeddingModel embeddingModel,
                            @Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        this.apiKey = apiKey;
        this.chatClient = builder
                .defaultSystem("""
                        你是一名 PDF 知识库助手。只能基于提供的 context 回答问题：
                        1. 若 context 里没有信息，请回答“抱歉，PDF 中未找到相关内容”。
                        2. 每条结论后面用 [page N] 标注引用的 PDF 页码，可综合多页。
                        3. 不允许猜测未在 context 里出现的具体数字或人名。
                        """)
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder().topK(6).similarityThreshold(0.3d).build())
                        .build())
                .build();
    }

    @Bean
    VectorStore pdfVectorStore() { return vectorStore; }

    /** 启动时如果 resources/pdf/sample.pdf 存在则自动灌库，方便零配置演示。 */
    @Bean
    public ApplicationRunner autoLoadIfPresent() {
        return args -> {
            File f = new File("src/main/resources/pdf/sample.pdf");
            if (f.exists()) {
                int chunks = ingest(new PathResource(f.toPath()), "sample.pdf");
                System.out.printf("[RAG] auto loaded sample.pdf, %d chunks%n", chunks);
            } else {
                System.out.println("[RAG] no sample.pdf found, upload via POST /pdf/upload to try");
            }
        };
    }

    /**
     * 上传 PDF 到知识库。curl -F 上传即可。
     */
    @PostMapping("/pdf/upload")
    public Object upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return Map.of("success", false, "error", "file must not be empty");
        Path tmp = Files.createTempFile("kb-", "-" + file.getOriginalFilename());
        file.transferTo(tmp.toFile());
        int chunks = ingest(new PathResource(tmp), file.getOriginalFilename());
        return Map.of("success", true, "fileName", file.getOriginalFilename(), "chunks", chunks);
    }

    /** 只检索不生成，方便查看命中的页码。 */
    @PostMapping("/pdf/retrieve")
    public Object retrieve(@RequestBody AskRequest req) {
        var hits = vectorStore.similaritySearch(SearchRequest.builder()
                .query(req.question()).topK(6).similarityThreshold(0.0d).build());
        return Map.of("question", req.question(), "hits",
                hits.stream().map(d -> Map.of(
                        "source", d.getMetadata().getOrDefault("file_name", "unknown"),
                        "page", d.getMetadata().getOrDefault(PagePdfDocumentReader.METADATA_START_PAGE_NUMBER, -1),
                        "text", truncate(d.getText(), 200)
                )).toList());
    }

    @PostMapping("/pdf/ask")
    public Object ask(@RequestBody AskRequest req) {
        if (apiKey == null || apiKey.isBlank()) return Map.of("success", false, "error", "DASHSCOPE_API_KEY is not configured");
        String answer = chatClient.prompt().user(req.question()).call().content();
        return Map.of("success", true, "question", req.question(), "answer", answer);
    }

    /** 具体的“读 PDF → 切片 → 灌库”流程。 */
    private int ingest(Resource pdf, String fileName) {
        PagePdfDocumentReader reader = new PagePdfDocumentReader(pdf,
                PdfDocumentReaderConfig.builder()
                        .withPagesPerDocument(1)          // 每页一个 Document，便于按 page 引用
                        .withPageTopMargin(0)
                        .withPageBottomMargin(0)
                        .build());
        List<Document> docs = reader.get();
        docs.forEach(d -> d.getMetadata().put("file_name", fileName));
        List<Document> chunks = new TokenTextSplitter(400, 200, 10, 5000, true).apply(docs);
        vectorStore.add(chunks);
        return chunks.size();
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    public record AskRequest(String question) {}
}
