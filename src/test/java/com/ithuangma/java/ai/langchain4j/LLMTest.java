package com.ithuangma.java.ai.langchain4j;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class LLMTest {
    @Test
    public void testGPTDemo() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("langchain4j.open-ai.chat-model.")
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .build();
        String chat = model.chat("你叫什么名字");
        System.out.println(chat);
    }
    /**
     * OpenAi
     */
    @Autowired
    private OpenAiChatModel openAiChatModel;
    //@Autowired
    //private ChatLanguageModel chatLanguageModel;
    @Test
    public void testDemo2() {
        String chat = openAiChatModel.chat("你是谁");
        System.out.println(chat);
    }
    /**
     * Ollama
     */
    @Autowired
    private OllamaChatModel ollamaChatModel;
    @Test
    public void testOllama() {
        String chat = ollamaChatModel.chat("你叫什么名字");
        System.out.println(chat);
    }
    /**
     * 通义千问大模型
     */
    @Autowired
    private QwenChatModel qwenChatModel;
    @Test
    public void testDashScopeQwen() {
        String answer = qwenChatModel.chat("你好");
        System.out.println(answer);
    }
}
