package com.ithuangma.java.ai.langchain4j.Story;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_sessions")
public class ChatSession {

    @Id
    private String id;

    private String sessionId;

    private String type; // PRIVATE or GROUP

    private List<String> participants;

    private String teamId; // If it's a group chat, associate with team ID

    private List<ChatMessage> messages;

    private Date lastActiveTime;
}
