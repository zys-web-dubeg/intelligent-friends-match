package com.ithuangma.java.ai.langchain4j.assistant;

import com.ithuangma.java.ai.langchain4j.Story.ChatSession;
import com.ithuangma.java.ai.langchain4j.entity.User;
import com.ithuangma.java.ai.langchain4j.Service.UserService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TeamChatAssistant {

    @Autowired
    @Qualifier("teamChatModel")
    private ChatLanguageModel chatModel;
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private UserService userService;

    /**
     * 生成团队聊天的AI回复
     * @param teamId 队伍ID
     * @param userId 发送消息的用户ID
     * @param message 消息内容
     * @return AI回复
     */
    public String generateTeamReply(String teamId, String userId, String message) {
        // 获取团队聊天上下文
        ChatSession chatSession = mongoTemplate.findById(teamId, ChatSession.class, "chat_sessions");
        
        if (chatSession == null || chatSession.getMessages() == null || chatSession.getMessages().isEmpty()) {
            // 如果没有历史记录，使用简单的回复
            return generateSimpleReply(message, teamId);
        }

        // 获取最近的聊天记录作为上下文
        List<com.ithuangma.java.ai.langchain4j.Story.ChatMessage> recentMessages = 
            chatSession.getMessages().subList(
                Math.max(0, chatSession.getMessages().size() - 10), // 获取最后10条消息
                chatSession.getMessages().size()
            );

        // 构建上下文消息列表
        java.util.List<ChatMessage> contextMessages = buildContextMessages(recentMessages, teamId);
        
        // 添加当前用户消息
        UserMessage currentUserMessage = UserMessage.from("用户" + userId + "说: " + message);
        contextMessages.add(currentUserMessage);

        // 手动构建完整的消息列表（包括系统消息、上下文消息和当前用户消息）
        java.util.List<ChatMessage> allMessages = new java.util.ArrayList<>();
        allMessages.add(SystemMessage.from(
            "你现在在一个团队聊天环境中，正在与多名成员交流。" +
            "请考虑上下文中的对话历史，并以友好、有助于团队协作的方式回复。" +
            "如果被提及，请回应；如果没有被直接提及，也可以根据话题适当参与讨论。"
        ));
        allMessages.addAll(contextMessages);
        
        // 使用 chatModel 直接生成回复
        return chatModel.chat(allMessages).aiMessage().text();
    }

    /**
     * 构建聊天上下文消息
     */
    private java.util.List<ChatMessage> buildContextMessages(
            List<com.ithuangma.java.ai.langchain4j.Story.ChatMessage> recentMessages, String teamId) {
        
        java.util.List<ChatMessage> contextMessages = recentMessages.stream()
                .map(msg -> {
                    String senderName = msg.getSenderId();
                    
                    // 如果是人类用户，尝试获取用户名
                    if ("HUMAN".equals(msg.getSenderType())) {
                        try {
                            Long userId = Long.parseLong(msg.getSenderId());
                            User user = userService.getById(userId);
                            if (user != null) {
                                senderName = user.getUsername();
                            }
                        } catch (NumberFormatException e) {
                            // 如果ID不是数字，保持原样
                            senderName = msg.getSenderId();
                        }
                    } else {
                        // 如果是AI，使用AI名称
                        senderName = "AI助手";
                    }
                    
                    String formattedMessage = senderName + ": " + msg.getContent();
                    
                    if ("HUMAN".equals(msg.getSenderType())) {
                        return UserMessage.from(formattedMessage);
                    } else {
                        return AiMessage.from(formattedMessage);
                    }
                })
                .collect(Collectors.toList());

        // 添加系统消息，告诉AI当前在团队环境中
        SystemMessage systemMessage = SystemMessage.from(
            "你现在在一个团队聊天环境中，正在与多名成员交流。" +
            "请考虑上下文中的对话历史，并以友好、有助于团队协作的方式回复。" +
            "如果被提及，请回应；如果没有被直接提及，也可以根据话题适当参与讨论。"
        );
        
        contextMessages.add(0, systemMessage); // 将系统消息放在开头
        
        return contextMessages;
    }

    /**
     * 生成简单回复（当没有上下文时）
     */
    private String generateSimpleReply(String message, String teamId) {
        java.util.List<ChatMessage> messages = new java.util.ArrayList<>();
        messages.add(SystemMessage.from(
            "你现在在一个团队聊天环境中。" +
            "请以友好、有助于团队协作的方式回复。"
        ));
        messages.add(UserMessage.from(message));
        
        // 使用 chatModel 直接生成回复
        return chatModel.chat(messages).aiMessage().text();
    }
}
