package com.ithuangma.java.ai.langchain4j.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ithuangma.java.ai.langchain4j.Story.ChatMessage;
import com.ithuangma.java.ai.langchain4j.Story.ChatSession;
import com.ithuangma.java.ai.langchain4j.assistant.TeamChatAssistant;
import com.ithuangma.java.ai.langchain4j.util.JwtUtils;
import com.ithuangma.java.ai.langchain4j.util.SpringContextUtil;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/websocket/team/{teamId}/{userId}")
@Component
public class TeamChatWebSocket {

    // 存储所有连接的会话
    private static CopyOnWriteArraySet<TeamChatWebSocket> webSocketSet = new CopyOnWriteArraySet<>();

    // 存储用户ID到WebSocket实例的映射
    private static ConcurrentHashMap<String, TeamChatWebSocket> connections = new ConcurrentHashMap<>();

    private Session session;
    private String teamId;
    private String userId;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private MongoTemplate mongoTemplate;

    public TeamChatWebSocket() {
        this.mongoTemplate = SpringContextUtil.getBean(MongoTemplate.class);
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("teamId") String teamId, @PathParam("userId") String userId) {
        // 验证JWT token
        String queryString = session.getQueryString();
        String token = null;
        if (queryString != null) {
            for (String param : queryString.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2 && "token".equals(pair[0])) {
                    token = java.net.URLDecoder.decode(pair[1], java.nio.charset.StandardCharsets.UTF_8);
                    break;
                }
            }
        }

        if (token == null || !JwtUtils.validateToken(token)) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "未授权访问"));
            } catch (IOException ignored) {
            }
            return;
        }

        this.session = session;
        this.teamId = teamId;
        this.userId = userId;
        webSocketSet.add(this);
        connections.put(teamId + "_" + userId, this);

        System.out.println("用户 " + userId + " 连接到队伍 " + teamId + "，当前连接数：" + webSocketSet.size());

        sendMessage("{\"type\":\"CONNECT\",\"message\":\"连接成功\",\"userId\":\"" + userId + "\",\"teamId\":\"" + teamId + "\"}");
    }

    @OnClose
    public void onClose() {
        webSocketSet.remove(this);
        if (teamId != null && userId != null) {
            connections.remove(teamId + "_" + userId);
            System.out.println("用户 " + userId + " 断开连接，当前连接数：" + webSocketSet.size());
        } else {
            // 只记录调试信息，不打印到控制台
            System.out.println("WebSocket连接关闭（未认证或临时连接），当前连接数：" + webSocketSet.size());
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("来自用户 " + userId + " 的消息：" + message);

        try {
            ChatMessageDto chatMessage = objectMapper.readValue(message, ChatMessageDto.class);

            // 根据消息类型进行不同处理
            if ("AI_TRIGGER".equals(chatMessage.getType())) {
                // 处理AI触发消息
                handleAITrigger(chatMessage.getContent());
            } else {
                // 处理普通消息
                handleNormalMessage(chatMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                Map<String, Object> errMsg = new HashMap<>();
                errMsg.put("type", "ERROR");
                errMsg.put("message", "消息发送失败: " + e.getMessage());
                sendMessage(objectMapper.writeValueAsString(errMsg));
            } catch (Exception ignored) {
            }
        }
    }

    // 处理普通消息
    private void handleNormalMessage(ChatMessageDto chatMessage) throws Exception {
        // 构建完整的聊天消息
        ChatMessage dbMessage = ChatMessage.builder()
                .senderId(userId)
                .senderType("HUMAN") // 标识是人类用户发送的
                .content(chatMessage.getContent())
                .timestamp(new java.util.Date())
                .build();

        // 检查会话是否存在，如果不存在则创建
        ChatSession chatSession = mongoTemplate.findById(teamId, ChatSession.class, "chat_sessions");
        if (chatSession == null) {
            // 创建新的会话
            chatSession = ChatSession.builder()
                    .id(teamId)
                    .sessionId(teamId)
                    .type("GROUP")
                    .teamId(teamId)
                    .messages(java.util.Collections.singletonList(dbMessage))
                    .lastActiveTime(new java.util.Date())
                    .build();
            mongoTemplate.save(chatSession, "chat_sessions");
        } else {
            // 添加消息到现有会话
            chatSession.getMessages().add(dbMessage);
            chatSession.setLastActiveTime(new java.util.Date());
            mongoTemplate.save(chatSession, "chat_sessions");
        }

        // 向队伍中的所有成员广播消息
        broadcastToTeam(teamId, userId, chatMessage.getContent(), dbMessage.getTimestamp());

        // 检查是否需要AI参与（例如，消息中包含特定关键词）
        if (shouldAIParticipate(chatMessage.getContent())) {
            // 异步生成AI回复，避免阻塞当前消息处理
            handleAITrigger(chatMessage.getContent());
        }
    }

    // 处理AI触发
    private void handleAITrigger(String content) {
        ChatSession chatSession = mongoTemplate.findById(teamId, ChatSession.class, "chat_sessions");
        if (chatSession == null) {
            // 如果没有会话，创建一个空会话
            chatSession = ChatSession.builder()
                    .id(teamId)
                    .sessionId(teamId)
                    .type("GROUP")
                    .teamId(teamId)
                    .messages(new java.util.ArrayList<>())
                    .lastActiveTime(new java.util.Date())
                    .build();
            mongoTemplate.save(chatSession, "chat_sessions");
        }

        // 异步生成AI回复，避免阻塞当前消息处理
        final ChatSession finalChatSession = chatSession;
        final String userContent = content;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                TeamChatAssistant teamChatAssistant = SpringContextUtil.getBean(TeamChatAssistant.class);
                if (teamChatAssistant != null) {
                    String aiReply = teamChatAssistant.generateTeamReply(teamId, "ai_assistant", userContent);

                    ChatMessage aiMessage = ChatMessage.builder()
                            .senderId("ai_assistant")
                            .senderType("AI")
                            .content(aiReply)
                            .timestamp(new java.util.Date())
                            .build();

                    finalChatSession.getMessages().add(aiMessage);
                    finalChatSession.setLastActiveTime(new java.util.Date());
                    mongoTemplate.save(finalChatSession, "chat_sessions");

                    sendAimessageToTeam(teamId, "AI助手: " + aiReply);
                }
            } catch (Exception e) {
                System.err.println("AI回复生成失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("用户 " + userId + " 发生错误：" + error.getMessage());
        error.printStackTrace();
    }

    // 发送消息给当前连接
    public void sendMessage(String message) {
        try {
            this.session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 向指定队伍的所有成员广播消息
    private static void broadcastToTeam(String teamId, String senderUserId, String content, Date timestamp) {
        try {
            Map<String, Object> wrapper = new HashMap<>();
            wrapper.put("senderUserId", senderUserId);
            wrapper.put("type", "MESSAGE");
            wrapper.put("senderType", "HUMAN");
            wrapper.put("content", content);
            wrapper.put("timestamp", timestamp.getTime());
            wrapper.put("senderId", senderUserId);
            String json = objectMapper.writeValueAsString(wrapper);

            for (TeamChatWebSocket item : webSocketSet) {
                if (item.teamId.equals(teamId)) {
                    try {
                        item.session.getBasicRemote().sendText(json);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 向指定用户发送消息
    public static void sendMessageToUser(String teamId, String userId, String message) {
        TeamChatWebSocket socket = connections.get(teamId + "_" + userId);
        if (socket != null) {
            socket.sendMessage(message);
        }
    }

    // 向指定队伍发送AI消息
    public static void sendAimessageToTeam(String teamId, String message) {
        try {
            Map<String, Object> aiMsg = new HashMap<>();
            aiMsg.put("type", "AI_MESSAGE");
            aiMsg.put("senderType", "AI");
            aiMsg.put("senderId", "ai_assistant");
            aiMsg.put("content", message);
            aiMsg.put("timestamp", System.currentTimeMillis());
            String json = objectMapper.writeValueAsString(aiMsg);

            for (TeamChatWebSocket item : webSocketSet) {
                if (item.teamId.equals(teamId)) {
                    try {
                        item.session.getBasicRemote().sendText(json);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 判断是否需要AI参与对话
    private boolean shouldAIParticipate(String content) {
        String lowerContent = content.toLowerCase();
        return lowerContent.contains("ai") ||
                lowerContent.contains("助手") ||
                lowerContent.contains("帮忙") ||
                lowerContent.contains("帮助") ||
                lowerContent.contains("建议") ||
                lowerContent.contains("意见") ||
                lowerContent.contains("怎么看") ||
                lowerContent.contains("想法") ||
                lowerContent.contains("推荐") ||
                lowerContent.contains("应该") ||
                lowerContent.contains("多少") ||
                lowerContent.contains("什么") ||
                lowerContent.contains("怎么") ||
                lowerContent.contains("为什么") ||
                lowerContent.contains("哪个") ||
                lowerContent.contains("请教") ||
                lowerContent.contains("告诉") ||
                lowerContent.contains("知道") ||
                lowerContent.contains("好不好") ||
                lowerContent.contains("对吗") ||
                lowerContent.contains("正确") ||
                lowerContent.contains("问题") ||
                lowerContent.contains("方法") ||
                lowerContent.contains("学习") ||
                content.contains("？") ||
                content.contains("?");
    }

    // 用于反序列化前端发送的消息

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatMessageDto {
        private String content;
        private String type;
        private String teamId;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTeamId() {
            return teamId;
        }

        public void setTeamId(String teamId) {
            this.teamId = teamId;
        }
    }
}