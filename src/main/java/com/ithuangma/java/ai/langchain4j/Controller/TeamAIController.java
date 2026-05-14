package com.ithuangma.java.ai.langchain4j.Controller;

import com.ithuangma.java.ai.langchain4j.assistant.TeamChatAssistant;
import com.ithuangma.java.ai.langchain4j.websocket.TeamChatWebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/team-ai")
public class TeamAIController {

    @Autowired
    private TeamChatAssistant teamChatAssistant;

    /**
     * AI助手在团队中回复消息
     */
    @PostMapping("/reply/{teamId}/{userId}")
    public String teamAiReply(@PathVariable String teamId, 
                             @PathVariable String userId, 
                             @RequestBody TeamMessageRequest request) {
        String aiReply = teamChatAssistant.generateTeamReply(teamId, userId, request.getMessage());
        
        // 将AI回复通过WebSocket发送到团队
        TeamChatWebSocket.sendAimessageToTeam(teamId, "AI助手: " + aiReply);
        
        return aiReply;
    }

    /**
     * AI主动加入团队对话（基于关键词或特定时机）
     */
    @PostMapping("/join-conversation/{teamId}")
    public String joinConversation(@PathVariable String teamId, 
                                  @RequestParam(required = false) String topic) {
        String aiReply = "大家好，我是AI助手，很高兴加入你们的讨论！";
        if (topic != null && !topic.trim().isEmpty()) {
            aiReply += "关于\"" + topic + "\"这个话题，我也有一些想法...";
        }
        
        // 将AI问候通过WebSocket发送到团队
        TeamChatWebSocket.sendAimessageToTeam(teamId, "AI助手: " + aiReply);
        
        return aiReply;
    }

    // 内部类用于处理请求
    public static class TeamMessageRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
