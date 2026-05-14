package com.ithuangma.java.ai.langchain4j.Config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingStoreConfig.class);
    private static final int MAX_RETRIES = 3;
    private static final int BASE_DELAY_MS = 2000;

    @Bean
    public EmbeddingModel embeddingModel() {
        return dev.langchain4j.community.model.dashscope.QwenEmbeddingModel.builder()
                .apiKey(System.getenv("DASH_SCOPE_API_KEY"))
                .modelName("text-embedding-v3")
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            EmbeddingModel embeddingModel) {

        String pineconeApiKey = System.getenv("PINECONE_API_KEY");
        if (pineconeApiKey == null || pineconeApiKey.isEmpty()) {
            log.warn("Pinecone API key not configured, falling back to InMemoryEmbeddingStore");
            return new InMemoryEmbeddingStore<>();
        }

        for (int i = 1; i <= MAX_RETRIES; i++) {
            try {
                log.info("Connecting to Pinecone (attempt {}/{})", i, MAX_RETRIES);
                PineconeEmbeddingStore store = PineconeEmbeddingStore.builder()
                        .apiKey(pineconeApiKey)
                        .index("friendmatch-index")
                        .nameSpace("friendmatch-namespace")
                        .build();
                
                // 测试连接是否可用
                log.info("Testing Pinecone connection...");
                dev.langchain4j.data.embedding.Embedding testEmbedding = embeddingModel.embed("test").content();
                var testRequest = dev.langchain4j.store.embedding.EmbeddingSearchRequest.builder()
                        .queryEmbedding(testEmbedding)
                        .maxResults(1)
                        .build();
                store.search(testRequest);
                
                log.info("✓ Successfully connected to Pinecone");
                return store;
            } catch (Exception e) {
                log.warn("Attempt {} failed: {}", i, e.getMessage());
                if (i < MAX_RETRIES) {
                    long delay = (long) BASE_DELAY_MS * i;
                    log.info("Retrying in {}ms...", delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.warn("All Pinecone connection attempts failed, falling back to InMemoryEmbeddingStore");
        return new InMemoryEmbeddingStore<>();
    }
}
