package com.example.springaialibaba.csagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 智能客服 Agent Controller。三件能力叠加：
 * 1. Memory：多轮上下文，按 conversationId 隔离。Spring AI 1.1 用 MessageWindowChatMemory 代替
 *    旧版 InMemoryChatMemory；BaseChatMemoryAdvisor 代替旧版 AbstractChatMemoryAdvisor。
 * 2. RAG：常见问题、退款政策等来自知识库（QuestionAnswerAdvisor）。
 * 3. Tool：订单查询、退款申请等业务动作。
 */
@Configuration
@RestController
public class CustomerServiceController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final String apiKey;

    public CustomerServiceController(ChatClient.Builder builder,
                                     EmbeddingModel embeddingModel,
                                     CustomerServiceTools tools,
                                     @Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        // Spring AI 1.1: MessageWindowChatMemory 带默认窗口大小（默认保留最近 20 条消息）。
        ChatMemory memory = MessageWindowChatMemory.builder().build();

        this.chatClient = builder
                .defaultSystem("""
                        你是一名电商智能客服，规则：
                        1. 涉及订单具体信息，必须调用工具 listMyOrders / getOrder / requestRefund；不要凭空回答。
                        2. 涉及退款政策 / 售后规则等非订单具体信息，参考知识库 context。
                        3. 一旦调用 requestRefund，最终回答必须明确"退款申请已提交，等待人工确认"，不要承诺已成功退款。
                        4. 传入工具的 currentUserId 只能来自会话上下文（服务端会通过 System 补充信息告知）。
                        5. 全程中文，先给结论再给依据。
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(memory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder().topK(4).similarityThreshold(0.3d).build())
                                .build())
                .defaultTools(tools)
                .build();
    }

    @Bean
    public ApplicationRunner loadCsKb() {
        return args -> {
            List<String> files = List.of(
                    "kb/refund-policy.md",
                    "kb/shipping-policy.md",
                    "kb/warranty.md");
            List<Document> chunks = new ArrayList<>();
            TokenTextSplitter splitter = new TokenTextSplitter(400, 200, 10, 5000, true);
            for (String path : files) {
                var res = new ClassPathResource(path);
                if (!res.exists()) continue;
                TextReader reader = new TextReader(res);
                reader.getCustomMetadata().put("source", path);
                chunks.addAll(splitter.apply(reader.get()));
            }
            vectorStore.add(chunks);
            System.out.printf("[CS-Agent] loaded %d chunks%n", chunks.size());
        };
    }

    @PostMapping("/cs/ask")
    public Object ask(@RequestHeader(value = "X-User-Id", required = false) String userId,
                      @RequestBody AskRequest req) {
        if (apiKey == null || apiKey.isBlank()) return Map.of("error", "DASHSCOPE_API_KEY is not configured");
        if (userId == null || userId.isBlank()) return Map.of("error", "X-User-Id header required");
        if (req == null || req.question() == null || req.question().isBlank())
            return Map.of("error", "question must not be blank");

        String cid = req.conversationId() == null ? userId + "-default" : req.conversationId();

        // 把 currentUserId 通过 System 消息追加告知模型；工具描述里已经写清楚这个参数从哪来。
        String user = "【会话上下文】当前登录用户 id 是 " + userId + "，如需调用工具请把它作为 currentUserId 参数传入。\n\n用户问题：" + req.question();

        try {
            String answer = chatClient.prompt()
                    .user(user)
                    // Spring AI 1.1 用字符串 key "chat_memory_conversation_id" 传递会话 ID；
                    // MessageWindowChatMemory 的窗口大小在构造时指定，不再通过 advisor param 传递。
                    .advisors(a -> a.param("chat_memory_conversation_id", cid))
                    .call().content();
            return Map.of("success", true, "userId", userId, "conversationId", cid,
                    "question", req.question(), "answer", answer);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public record AskRequest(String conversationId, String question) {}
}
