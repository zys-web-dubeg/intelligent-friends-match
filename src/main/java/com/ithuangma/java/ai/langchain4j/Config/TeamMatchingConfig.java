package com.ithuangma.java.ai.langchain4j.Config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TeamMatchingConfig {
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;
    
    /**
     * 创建队伍特征内容检索器
     * 用于根据用户画像检索最匹配的队伍
     */
    @Bean
    public dev.langchain4j.rag.content.retriever.ContentRetriever teamContentRetriever() {
        return dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
                .builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(5)  // 最多返回5个匹配的队伍
                .minScore(0.6)  // 最低相似度阈值
                .build();
    }
}
