package com.ithuangma.java.ai.langchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

/**
 * 智能伙伴匹配助手
 * 核心AI助手，帮助用户通过对话找到最合适的伙伴和队伍
 * 配备伙伴匹配相关的Tools，AI会根据用户意图自动选择合适的工具调用
 */
@AiService(wiringMode = EXPLICIT,
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "chatMemoryProviderPartnerMatching",
        tools = "partnerMatchingTools",
        contentRetriever = "contentRetrieverPartnerMatching")
public interface PartnerMatchingAgent {

    @SystemMessage(fromResource = "partner-matching-prompt-template.txt")
    Flux<String> chat(@MemoryId Long memoryId, @UserMessage String message);
}