package com.ithuangma.java.ai.langchain4j;

import com.ithuangma.java.ai.langchain4j.assistant.MemoryChatAssistant;
import com.ithuangma.java.ai.langchain4j.assistant.SeparateChatAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PromptTest {
    @Autowired
    private SeparateChatAssistant separateChatAssistant;
    @Autowired
    private MemoryChatAssistant memoryChatAssistant;
    @Test
    public void testSystemMessage() {
        String answer = separateChatAssistant.chat(3, "我是谁啊，认识这么久记得我的名字吗，今天的日期是什么啊");
        System.out.println(answer);
    }
    @Test
    public void testSystemMessage2() {
        String answer = separateChatAssistant.chat(4, "我是谁啊，认识这么久记得我的名字吗，今天的日期是什么啊");
        System.out.println(answer);
    }

    @Test
    public void testUserMessage() {
        String answer = memoryChatAssistant.chat("我是王林，外号王麻子");
        System.out.println(answer);
        String answer2 = memoryChatAssistant.chat("我是王林，外号是啥");
        System.out.println(answer2);
        String answer3 = memoryChatAssistant.chat("我的外号是王麻子，你猜我是谁");
        System.out.println(answer3);

    }
    @Test
    public void testChat2() {
        String answer = separateChatAssistant.chat2(7, "我是谁，我多大了", "王林", 18);
        System.out.println(answer);
    }
}