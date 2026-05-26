package com.ithuangma.java.ai.langchain4j.Story;
import com.ithuangma.java.ai.langchain4j.Bean.ChatMessages;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.*;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
public class MongoChatMemoryStory implements ChatMemoryStore {
    @Autowired
    private MongoTemplate mongoTemplate;

    //    @Override
//    public List<ChatMessage> getMessages(Object memoryId) {
//
//        Criteria criteria = Criteria.where("memoryId").is(memoryId);
//        Query query = new Query(criteria);
//
//        ChatMessages chatMessages = mongoTemplate.findOne(query, ChatMessages.class);
//        if (chatMessages == null || chatMessages.getContent() == null) {
//            return new ArrayList<>();
//        }
//        String contentJson = chatMessages.getContent();
//        return ChatMessageDeserializer.messagesFromJson(contentJson);
//    }
//
//    @Override
//    public void updateMessages(Object memoryId, List<ChatMessage> list) {
//        Criteria criteria = Criteria.where("memoryId").is(memoryId);
//        Query query = new Query(criteria);
//        Update update = new Update();
//        update.set("content", ChatMessageSerializer.messagesToJson(list));
//        mongoTemplate.upsert(query, update, ChatMessages.class);
//    }
//
//    @Override
//    public void deleteMessages(Object memoryId) {
//        Criteria criteria = Criteria.where("memoryId").is(memoryId);
//        Query query = new Query(criteria);
//        mongoTemplate.remove(query, ChatMessages.class);
//    }
    @Override
    public List<dev.langchain4j.data.message.ChatMessage> getMessages(Object memoryId) {
        // 查询特定会话的聊天记�?
        Query query = new Query(Criteria.where("sessionId").is(memoryId.toString()));

        ChatSession session = mongoTemplate.findOne(query, ChatSession.class);

        if (session == null || session.getMessages() == null) {
            return new ArrayList<>();
        }

        // 将MongoDB中的消息转换为LangChain4j的消息格�?
        return session.getMessages().stream()
                .map(dbMsg -> {
                    String content = dbMsg.getContent();
                    if (content == null || content.isEmpty()) {
                        content = "";
                    }

                    if ("HUMAN".equals(dbMsg.getSenderType())) {
                        return UserMessage.from(content);
                    } else if ("SYSTEM".equals(dbMsg.getSenderType())) {
                        return SystemMessage.from(content);
                    } else if ("TOOL".equals(dbMsg.getSenderType())) {
                        // 恢复工具执行结果消息
                        Map<String, Object> meta = dbMsg.getMeta();
                        if (meta != null && meta.containsKey("toolName")) {
                            String toolName = (String) meta.get("toolName");
                            String toolExecId = (String) meta.getOrDefault("toolExecutionId", "");
                            ToolExecutionRequest request = ToolExecutionRequest.builder()
                                    .id(toolExecId)
                                    .name(toolName)
                                    .arguments("")
                                    .build();
                            return ToolExecutionResultMessage.from(request, content);
                        }
                        return AiMessage.from(content);
                    } else {
                        return AiMessage.from(content);
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public void updateMessages(Object memoryId, List<dev.langchain4j.data.message.ChatMessage> messages) {
        // 查询会话
        Query query = new Query(Criteria.where("sessionId").is(memoryId.toString()));

        // 将LangChain4j消息转换为数据库格式并更�?
        List<com.ithuangma.java.ai.langchain4j.Story.ChatMessage> convertedMessages = messages.stream()
                .map(langMsg -> {
                    com.ithuangma.java.ai.langchain4j.Story.ChatMessage dbMsg = new com.ithuangma.java.ai.langchain4j.Story.ChatMessage();
                    if (langMsg instanceof UserMessage) {
                        dbMsg.setSenderId("human");
                        dbMsg.setSenderType("HUMAN");
                        String text = ((UserMessage) langMsg).singleText();
                        dbMsg.setContent(text != null ? text : "");
                    } else if (langMsg instanceof AiMessage) {
                        AiMessage aiMsg = (AiMessage) langMsg;
                        dbMsg.setSenderId("ai");
                        dbMsg.setSenderType("AI");
                        String text = aiMsg.text();
                        dbMsg.setContent(text != null ? text : "");
                        // 保存工具调用请求信息，以便在getMessages中恢�?
                        if (aiMsg.hasToolExecutionRequests()) {
                            List<ToolExecutionRequest> requests = aiMsg.toolExecutionRequests();
                            if (requests != null && !requests.isEmpty()) {
                                ToolExecutionRequest req = requests.get(0);
                                Map<String, Object> meta = new HashMap<>();
                                meta.put("toolExecutionId", req.id());
                                meta.put("toolName", req.name());
                                meta.put("toolArguments", req.arguments());
                                dbMsg.setMeta(meta);
                            }
                        }
                    } else if (langMsg instanceof SystemMessage) {
                        dbMsg.setSenderId("system");
                        dbMsg.setSenderType("SYSTEM");
                        String text = ((SystemMessage) langMsg).text();
                        dbMsg.setContent(text != null ? text : "");
                    } else if (langMsg instanceof ToolExecutionResultMessage) {
                        ToolExecutionResultMessage toolMsg = (ToolExecutionResultMessage) langMsg;
                        dbMsg.setSenderId("tool");
                        dbMsg.setSenderType("TOOL");
                        dbMsg.setContent(toolMsg.text() != null ? toolMsg.text() : "");
                        // 保存工具执行元数�?
                        Map<String, Object> meta = new HashMap<>();
                        meta.put("toolExecutionId", toolMsg.id());
                        meta.put("toolName", toolMsg.toolName());
                        dbMsg.setMeta(meta);
                    } else {
                        // 跳过不支持的消息类型
                        return null;
                    }
                    dbMsg.setTimestamp(new java.util.Date());
                    return dbMsg;
                })
                .filter(msg -> msg != null)
                .collect(Collectors.toList());

        // 更新或插入会�?
        Update update = new Update()
                .set("messages", convertedMessages)
                .set("lastActiveTime", new java.util.Date())
                .set("sessionId", memoryId.toString());

        // 从当前请求上下文中获�?userId �?aiId（私聊会话）
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                Object userIdAttr = request.getAttribute("userId");
                if (userIdAttr != null) {
                    update.set("userId", userIdAttr.toString());
                }
                // 根据请求 URL 路径确定 AI 助手 ID
                String uri = request.getRequestURI();
                if (uri.contains("/api/xiaozhi/")) {
                    update.set("aiId", "zhangzhikang");
                } else if (uri.contains("/api/shopping/")) {
                    update.set("aiId", "yangnan");
                } else if (uri.contains("/api/love/")) {
                    update.set("aiId", "tuzhixing");
                } else if (uri.contains("/api/learning/")) {
                    update.set("aiId", "learning");
                }
            }
        } catch (Exception e) {
            // �?HTTP 请求上下文（如后台任务），忽�?
        }

        mongoTemplate.upsert(query, update, ChatSession.class);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        Query query = new Query(Criteria.where("sessionId").is(memoryId.toString()));
        mongoTemplate.remove(query, ChatSession.class);
    }
}

