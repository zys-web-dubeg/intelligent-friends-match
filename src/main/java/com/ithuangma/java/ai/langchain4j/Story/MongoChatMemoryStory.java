package com.ithuangma.java.ai.langchain4j.Story;
import com.ithuangma.java.ai.langchain4j.Bean.ChatMessages;
import dev.langchain4j.data.message.*;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
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
    // 查询特定会话的聊天记录
    Query query = new Query(Criteria.where("sessionId").is(memoryId.toString()));

    ChatSession session = mongoTemplate.findOne(query, ChatSession.class);

    if (session == null || session.getMessages() == null) {
        return new ArrayList<>();
    }

    // 将MongoDB中的消息转换为LangChain4j的消息格式
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

        // 将LangChain4j消息转换为数据库格式并更新
        List<com.ithuangma.java.ai.langchain4j.Story.ChatMessage> convertedMessages = messages.stream()
                .map(langMsg -> {
                    com.ithuangma.java.ai.langchain4j.Story.ChatMessage dbMsg = new com.ithuangma.java.ai.langchain4j.Story.ChatMessage();
                    if (langMsg instanceof UserMessage) {
                        dbMsg.setSenderId("human");
                        dbMsg.setSenderType("HUMAN");
                        String text = ((UserMessage) langMsg).singleText();
                        dbMsg.setContent(text != null ? text : "");
                    } else if (langMsg instanceof AiMessage) {
                        dbMsg.setSenderId("ai");
                        dbMsg.setSenderType("AI");
                        String text = ((AiMessage) langMsg).text();
                        dbMsg.setContent(text != null ? text : "");
                    } else if (langMsg instanceof SystemMessage) {
                        dbMsg.setSenderId("system");
                        dbMsg.setSenderType("SYSTEM");
                        String text = ((SystemMessage) langMsg).text();
                        dbMsg.setContent(text != null ? text : "");
                    } else {
                        // 跳过不支持的消息类型
                        return null;
                    }
                    dbMsg.setTimestamp(new java.util.Date());
                    return dbMsg;
                })
                .filter(msg -> msg != null)
                .collect(Collectors.toList());

        // 更新或插入会话
        Update update = new Update()
                .set("messages", convertedMessages)
                .set("lastActiveTime", new java.util.Date())
                .set("sessionId", memoryId.toString());

        mongoTemplate.upsert(query, update, ChatSession.class);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        Query query = new Query(Criteria.where("sessionId").is(memoryId.toString()));
        mongoTemplate.remove(query, ChatSession.class);
    }
}
