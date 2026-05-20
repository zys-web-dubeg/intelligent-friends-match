package com.ithuangma.java.ai.langchain4j.Controller;

import com.ithuangma.java.ai.langchain4j.Bean.Result;
import com.ithuangma.java.ai.langchain4j.Story.ChatMessage;
import com.ithuangma.java.ai.langchain4j.Story.ChatSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat-history")
public class ChatHistoryController {

    @Autowired
    private MongoTemplate mongoTemplate;

    // 获取队伍聊天记录
    @GetMapping("/team/{teamId}")
    public Result getTeamChatHistory(@PathVariable String teamId,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) String beforeTimestamp) {
        try {
            // 构建查询条件
            Query query = new Query(Criteria.where("sessionId").is(teamId));
            
            // 如果指定了时间戳，则只获取该时间之前的消息（用于分页）
            if (beforeTimestamp != null && !beforeTimestamp.isEmpty()) {
                query.addCriteria(Criteria.where("lastActiveTime").lt(java.util.Date.from(
                    java.time.Instant.parse(beforeTimestamp))));
            }
            
            // 按时间倒序排列，获取最新的消息
            query.with(Sort.by(Sort.Direction.DESC, "lastActiveTime"));
            query.skip(page * size);
            query.limit(size);
            
            ChatSession session = mongoTemplate.findOne(query, ChatSession.class, "chat_sessions");
            
            if (session != null && session.getMessages() != null) {
                // 按时间正序排列返回（最新的在最后）
                List<ChatMessage> messages = session.getMessages();
                messages.sort((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()));
                
                return Result.ok(messages);
            } else {
                return Result.ok(List.of());
            }
        } catch (Exception e) {
            return Result.fail(500, "获取聊天记录失败: " + e.getMessage());
        }
    }

    // 更准确的实现
    @GetMapping("/user/{userId}/ai/{aiId}")
    public Result getUserAiChatHistory(@PathVariable String userId,
                                       @PathVariable String aiId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        try {
            // 查找该用户的所有聊天会话（sessionId可能与用户相关）
            // 在实际应用中，memoryId可能是用户ID或基于用户ID生成的
            Query query = new Query(Criteria.where("sessionId").regex(userId + ".*")); // 匹配以userId开头的sessionId
            query.with(Sort.by(Sort.Direction.DESC, "lastActiveTime"));
            query.skip(page * size);
            query.limit(size);

            List<ChatSession> sessions = mongoTemplate.find(query, ChatSession.class, "chat_sessions");

            // 合并所有相关消息
            java.util.List<ChatMessage> allMessages = new java.util.ArrayList<>();
            for (ChatSession session : sessions) {
                if (session.getMessages() != null) {
                    // 可以根据需要过滤特定AI的消息
                    allMessages.addAll(session.getMessages());
                }
            }

            // 按时间排序
            allMessages.sort(java.util.Comparator.comparing(ChatMessage::getTimestamp));

            return Result.ok(allMessages);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "获取聊天记录失败: " + e.getMessage());
        }
    }

    // 获取用户总的聊天统计
    @GetMapping("/user/{userId}/stats")
    public Result getUserChatStats(@PathVariable Long userId) {
        try {
            // 统计用户参与的队伍数量
            long teamCount = mongoTemplate.count(
                new Query(Criteria.where("participants").is(userId.toString())), 
                "chat_sessions"
            );
            
            // 统计用户总消息数
            // 这里需要遍历所有包含该用户的消息
            java.util.List<ChatSession> userSessions = mongoTemplate.find(
                new Query(Criteria.where("participants").in(userId.toString())), 
                ChatSession.class, 
                "chat_sessions"
            );
            
            long messageCount = 0;
            for (ChatSession session : userSessions) {
                if (session.getMessages() != null) {
                    messageCount += session.getMessages().size();
                }
            }
            
            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("teamCount", teamCount);
            stats.put("messageCount", messageCount);
            
            return Result.ok(stats);
        } catch (Exception e) {
            return Result.fail(500, "获取聊天统计失败: " + e.getMessage());
        }
    }

    // 清除过期的聊天记录
    @DeleteMapping("/cleanup")
    public Result cleanupExpiredChats(@RequestParam String retentionDays) {
        try {
            int days = Integer.parseInt(retentionDays);
            java.util.Date cutoffDate = new java.util.Date(
                System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
            );
            
            // 删除早于截止日期的聊天记录
            Query query = new Query(Criteria.where("lastActiveTime").lt(cutoffDate));
            long deletedCount = mongoTemplate.remove(query, "chat_sessions").getDeletedCount();
            
            return Result.ok("清理了 " + deletedCount + " 条过期聊天记录");
        } catch (Exception e) {
            return Result.fail(500, "清理聊天记录失败: " + e.getMessage());
        }
    }
}
