package com.ithuangma.java.ai.langchain4j.Config;

import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DashScopeConfig {

    @Bean
    public QwenStreamingChatModel qwenStreamingChatModel() {
        return QwenStreamingChatModel.builder()
                .apiKey(System.getenv("DASH_SCOPE_API_KEY"))
                .modelName("qwen-max")
                .build();
    }
}
