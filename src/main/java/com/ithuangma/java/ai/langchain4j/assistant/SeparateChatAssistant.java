package com.ithuangma.java.ai.langchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        chatModel = "qwenChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = "calculatorTools" //配置tools
)
public interface SeparateChatAssistant {
    /**
     * 分离聊天记录
     * @param memoryId 聊天id
     * @param userMessage 用户消息
     * @return
     */
    //@SystemMessage("你是我的好朋友，请用东北话回答问题。今天是{{current_date}}")//系统提示语
    @SystemMessage(fromResource = "prompt-template.txt")//系统提示语
    String chat(@MemoryId int memoryId, @UserMessage String userMessage);

    @SystemMessage(fromResource = "prompt-template2.txt")
    String chat2(@MemoryId int memoryId,
                 @UserMessage String userMessage,
                 @V("username") String username,
                 @V("age") int age);
}
