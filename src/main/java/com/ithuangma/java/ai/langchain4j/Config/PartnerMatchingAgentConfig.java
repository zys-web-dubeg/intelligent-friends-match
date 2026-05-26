package com.ithuangma.java.ai.langchain4j.Config;

import com.ithuangma.java.ai.langchain4j.Story.MongoChatMemoryStory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 智能伙伴匹配助手的配置类
 * 配置ChatMemory（对话记忆）和ContentRetriever（向量检索）
 */
@Configuration
public class PartnerMatchingAgentConfig {

    @Autowired
    private MongoChatMemoryStory mongoChatMemoryStory;

    @Autowired
    private EmbeddingStore embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    /**
     * 配置伙伴匹配助手的对话记忆
     * 使用MongoDB持久化，每个用户独立记忆（memoryId隔离）
     * maxMessages=50：保留最近50轮对话，确保上下文完整
     */
    @Bean
    public ChatMemoryProvider chatMemoryProviderPartnerMatching() {
        return memoryId ->
                MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(50)
                        .chatMemoryStore(mongoChatMemoryStory)
                        .build();
    }

    /**
     * 配置伙伴匹配助手的向量内容检索器
     * 用于从向量数据库中检索伙伴匹配知识、性格分析等信息
     * maxResults=3：最多返回3条相关内容
     * minScore=0.7：最低相似度0.7，保证检索质量
     */
    @Bean
    ContentRetriever contentRetrieverPartnerMatching() {
        return EmbeddingStoreContentRetriever
                .builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(3)
                .minScore(0.7)
                .build();
    }
}