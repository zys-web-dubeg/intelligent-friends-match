package com.ithuangma.java.ai.langchain4j;

import com.ithuangma.java.ai.langchain4j.assistant.Assistant;
import com.ithuangma.java.ai.langchain4j.assistant.MemoryChatAssistant;
import com.ithuangma.java.ai.langchain4j.assistant.SeparateChatAssistant;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AIServiceTest {
    @Autowired
    private QwenChatModel qwenChatModel;
    @Test
    public void testChat() {
        //创建AIService
        Assistant assistant = AiServices.create(Assistant.class, qwenChatModel);
        //调用service的接口
        String answer = assistant.chat("你是谁");
        System.out.println(answer);
    }
    @Autowired
    private Assistant assistant;
    @Test
    public void testChat2() {
        String answer = assistant.chat("我是谁");
        System.out.println(answer);
    }
    @Test
    public void testChat3() {

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(qwenChatModel)
                .chatMemory(chatMemory)
                .build();
        String answer1 = assistant.chat("我叫松松");
        System.out.println(answer1);
        String answer2 = assistant.chat("我叫什么名字");
        System.out.println(answer2);
    }

    @Autowired
    private MemoryChatAssistant memoryChatAssistant;
    @Test
    public void testChat4() {

        String answer1 = memoryChatAssistant.chat("我叫松松");
        System.out.println(answer1);
        String answer2 = memoryChatAssistant.chat("我叫什么名字");
        System.out.println(answer2);
    }
    @Autowired
    private SeparateChatAssistant separateAssistant;
    @Test
    public void testChat5() {
        String answer1 = separateAssistant.chat(1, "我叫松松");
        System.out.println(answer1);
        String answer2 = separateAssistant.chat(1, "我叫什么名字");
        System.out.println(answer2);
        String answer3 = separateAssistant.chat(2, "我叫什么名字");
        System.out.println(answer3);
    }
}
