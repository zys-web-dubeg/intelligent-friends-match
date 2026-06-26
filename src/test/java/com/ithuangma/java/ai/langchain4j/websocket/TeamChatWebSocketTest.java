package com.ithuangma.java.ai.langchain4j.websocket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamChatWebSocketTest {

    @Test
    void normalizeAiMessageRemovesRepeatedAssistantPrefixes() {
        assertEquals("嗨！有什么我可以帮忙的吗？",
                TeamChatWebSocket.normalizeAiMessageContent("AI助手: AI助手: 嗨！有什么我可以帮忙的吗？"));
    }

    @Test
    void normalizeAiMessageRemovesDifferentAiNamePrefixes() {
        assertEquals("我可以帮你确认一下。",
                TeamChatWebSocket.normalizeAiMessageContent("AI小智：AI助手: 我可以帮你确认一下。"));
    }

    @Test
    void normalizeAiMessageKeepsRegularContent() {
        assertEquals("大家好，我是队伍聊天助手。",
                TeamChatWebSocket.normalizeAiMessageContent("大家好，我是队伍聊天助手。"));
    }
}
