package com.ithuangma.java.ai.langchain4j.Config;

import com.ithuangma.java.ai.langchain4j.Story.MongoChatMemoryStory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeparateChatAssistantConfig {
    @Autowired
    private MongoChatMemoryStory mongoChatMemoryStory;
    @Bean
    public ChatMemoryProvider chatMemoryProvider(){
        return memoryId -> MessageWindowChatMemory
                .builder()
                .id(memoryId)
                .maxMessages(10)
                //.chatMemoryStore(new MongoChatMemoryStory())
                .chatMemoryStore(mongoChatMemoryStory)
                .build();
    }
}
