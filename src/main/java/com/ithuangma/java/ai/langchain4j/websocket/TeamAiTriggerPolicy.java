package com.ithuangma.java.ai.langchain4j.websocket;

import org.springframework.stereotype.Component;

@Component
public class TeamAiTriggerPolicy {

    public boolean shouldAIParticipate(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        String normalized = content.trim().toLowerCase();
        return normalized.contains("@ai") || normalized.contains("@助手");
    }
}
