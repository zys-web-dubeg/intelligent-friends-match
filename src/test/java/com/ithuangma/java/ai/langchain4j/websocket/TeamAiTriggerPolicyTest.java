package com.ithuangma.java.ai.langchain4j.websocket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamAiTriggerPolicyTest {

    private final TeamAiTriggerPolicy policy = new TeamAiTriggerPolicy();

    @Test
    void triggersWhenAiIsExplicitlyMentioned() {
        assertTrue(policy.shouldAIParticipate("@AI 帮我总结一下刚才的讨论"));
        assertTrue(policy.shouldAIParticipate("@ai 给个建议"));
        assertTrue(policy.shouldAIParticipate("@助手 这个方案怎么样"));
    }

    @Test
    void doesNotTriggerForOrdinaryQuestions() {
        assertFalse(policy.shouldAIParticipate("这个问题怎么解决？"));
        assertFalse(policy.shouldAIParticipate("今晚学习什么？"));
        assertFalse(policy.shouldAIParticipate("你知道他去哪了吗?"));
    }

    @Test
    void doesNotTriggerForLooseKeywordsWithoutMention() {
        assertFalse(policy.shouldAIParticipate("大家给点建议"));
        assertFalse(policy.shouldAIParticipate("这个方法对吗"));
        assertFalse(policy.shouldAIParticipate("我需要帮忙"));
    }

    @Test
    void doesNotTriggerForBlankContent() {
        assertFalse(policy.shouldAIParticipate(null));
        assertFalse(policy.shouldAIParticipate(""));
        assertFalse(policy.shouldAIParticipate("   "));
    }
}
