package com.ithuangma.java.ai.langchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;


@AiService(wiringMode = EXPLICIT,
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "chatMemoryProviderLove",
        tools = {}, // 恋爱智能体暂时不需要特殊工具
        contentRetriever = "contentRetrieverLove")
public interface LoveAgent {
    @SystemMessage(fromResource = "love-prompt-template.txt")
    Flux<String> chat(@MemoryId Long memoryId, @UserMessage String message);
}
