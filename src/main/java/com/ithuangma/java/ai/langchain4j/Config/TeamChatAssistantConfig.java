package com.ithuangma.java.ai.langchain4j.Config;

import com.ithuangma.java.ai.langchain4j.assistant.TeamChatAssistant;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TeamChatAssistantConfig {

    @Bean
    public TeamChatAssistant teamChatAssistant(@Qualifier("teamChatModel") ChatLanguageModel chatLanguageModel) {
        return new TeamChatAssistant();
    }

    // 如果需要特定的团队聊天模型配置
    @Bean("teamChatModel")
    public ChatLanguageModel teamChatModel() {
        return OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.1")
                .temperature(0.7) // 稍微提高温度，使AI回复更多样化
                .build();
    }
}