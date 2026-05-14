package com.ithuangma.java.ai.langchain4j.Controller;

import com.ithuangma.java.ai.langchain4j.Story.ChatMessage;
import com.ithuangma.java.ai.langchain4j.Story.ChatSession;
import com.ithuangma.java.ai.langchain4j.assistant.XiaozhiAgent;
import com.ithuangma.java.ai.langchain4j.entity.User;
import com.ithuangma.java.ai.langchain4j.Service.UserService;
import com.ithuangma.java.ai.langchain4j.Bean.Result;
import com.ithuangma.java.ai.langchain4j.websocket.TeamChatWebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/v1/chat")
public class TeamChatController {

    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private XiaozhiAgent xiaozhiAgent;

    // AI助手响应队伍聊天
    @PostMapping("/team/{teamId}/ai-response")
    public Result handleAiResponse(@PathVariable String teamId, @RequestBody ChatMessage message) {
        try {
            // 获取队伍最新的几条消息作为上下文
            ChatSession chatSession = mongoTemplate.findById(teamId, ChatSession.class, "chat_sessions");
            if (chatSession == null || chatSession.getMessages() == null || chatSession.getMessages().isEmpty()) {
                return Result.fail(404, "找不到聊天记录");
            }

            // 获取最后几条消息作为AI的上下文
            int startIndex = Math.max(0, chatSession.getMessages().size() - 5); // 取最后5条消息
            java.util.List<ChatMessage> recentMessages = chatSession.getMessages().subList(startIndex, chatSession.getMessages().size());
            
            // 构建对话上下文字符串
            StringBuilder contextBuilder = new StringBuilder();
            for (ChatMessage msg : recentMessages) {
                String senderName = "AI";
                if ("HUMAN".equals(msg.getSenderType())) {
                    User user = userService.getById(Long.valueOf(msg.getSenderId())); // 假设有此方法
                    senderName = user != null ? user.getUsername() : "用户";
                }
                contextBuilder.append(senderName).append(": ").append(msg.getContent()).append("\n");
            }

            // 让AI助手根据上下文生成回复
            String aiResponse = xiaozhiAgent.chat(Long.parseLong(teamId), contextBuilder.toString() + "\n请回复以上对话:").blockLast();
            
            // 创建AI回复的消息
            ChatMessage aiMessage = ChatMessage.builder()
                    .senderId("ai_xiaozhi") // AI助手ID
                    .senderType("AI")
                    .content(aiResponse)
                    .timestamp(new Date())
                    .build();

            // 保存AI的回复到数据库
            chatSession.getMessages().add(aiMessage);
            chatSession.setLastActiveTime(new Date());
            mongoTemplate.save(chatSession, "chat_sessions");

            // 通过WebSocket向队伍广播AI的回复
            TeamChatWebSocket.sendAimessageToTeam(teamId, "AI小智: " + aiResponse);

            return Result.ok(aiResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "AI回复失败: " + e.getMessage());
        }
    }
}