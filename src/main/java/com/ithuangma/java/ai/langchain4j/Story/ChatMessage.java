package com.ithuangma.java.ai.langchain4j.Story;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_messages")
public class ChatMessage {

    @Id
    private String id;

    private String senderId;

    private String senderType; // HUMAN or AI

    private String content;

    private Date timestamp;

    private Map<String, Object> meta; // Additional metadata like emotion
}
