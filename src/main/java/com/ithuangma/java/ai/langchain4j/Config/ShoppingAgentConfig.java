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

@Configuration
public class ShoppingAgentConfig {

    @Autowired
    private MongoChatMemoryStory mongoChatMemoryStory;

    @Autowired
    private EmbeddingStore embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Bean
    public ChatMemoryProvider chatMemoryProviderShopping() {
        return memoryId ->
                MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(50)
                        .chatMemoryStore(mongoChatMemoryStory)
                        .build();
    }

    @Bean
    ContentRetriever contentRetrieverShopping() {
        return EmbeddingStoreContentRetriever
                .builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(2)
                .minScore(0.7)
                .build();
    }
}