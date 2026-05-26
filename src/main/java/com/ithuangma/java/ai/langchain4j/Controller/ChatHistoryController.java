package com.ithuangma.java.ai.langchain4j.Controller;

import com.ithuangma.java.ai.langchain4j.Bean.Result;
import com.ithuangma.java.ai.langchain4j.Story.ChatMessage;
import com.ithuangma.java.ai.langchain4j.Story.ChatSession;
import com.ithuangma.java.ai.langchain4j.entity.TeamMemberRelation;
import com.ithuangma.java.ai.langchain4j.mapper.TeamMemberRelationMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/chat-history")
public class ChatHistoryController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TeamMemberRelationMapper teamMemberRelationMapper;

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

    // 获取用户与AI的私聊记录
    @GetMapping("/user/{userId}/ai/{aiId}")
    public Result getUserAiChatHistory(@PathVariable String userId,
                                       @PathVariable String aiId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        try {
            // 按 userId 和 aiId 精确查询私聊会话（由 MongoChatMemoryStory 在保存消息时记录）
            Criteria criteria = Criteria.where("userId").is(userId)
                    .and("aiId").is(aiId);
            Query query = new Query(criteria);
            query.with(Sort.by(Sort.Direction.DESC, "lastActiveTime"));
            query.skip(page * size);
            query.limit(size);

            List<ChatSession> sessions = mongoTemplate.find(query, ChatSession.class, "chat_sessions");

            // 合并所有相关消息
            List<ChatMessage> allMessages = new ArrayList<>();
            for (ChatSession session : sessions) {
                if (session.getMessages() != null) {
                    allMessages.addAll(session.getMessages());
                }
            }

            // 按时间排序
            allMessages.sort((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()));

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
            // 统计用户参与的队伍数量（从 team_member_relation 表获取真实数据）
            QueryWrapper<TeamMemberRelation> teamQuery = new QueryWrapper<>();
            teamQuery.eq("user_id", userId);
            long teamCount = teamMemberRelationMapper.selectCount(teamQuery);

            // 获取用户所属的所有队伍ID
            List<TeamMemberRelation> relations = teamMemberRelationMapper.selectList(teamQuery);
            List<String> userTeamIds = relations.stream()
                    .map(r -> r.getTeamId().toString())
                    .toList();

            // 统计用户总消息数 - 同时查询两种方式以确保兼容已有数据
            // 方式1: participants 包含该用户的会话
            // 方式2: 用户所属队伍的聊天会话（通过 teamId）
            List<Criteria> orCriteria = new ArrayList<>();
            orCriteria.add(Criteria.where("participants").in(userId.toString()));
            if (!userTeamIds.isEmpty()) {
                orCriteria.add(Criteria.where("teamId").in(userTeamIds));
            }

            Criteria criteria = new Criteria().orOperator(orCriteria.toArray(new Criteria[0]));
            List<ChatSession> userSessions = mongoTemplate.find(
                    new Query(criteria),
                    ChatSession.class,
                    "chat_sessions"
            );

            long messageCount = 0;
            // 使用 Set 对 sessionId 去重（同一个会话可能被两种条件同时命中）
            Set<String> countedSessionIds = new HashSet<>();
            for (ChatSession session : userSessions) {
                String sessionId = session.getSessionId();
                if (sessionId != null && countedSessionIds.contains(sessionId)) {
                    continue;
                }
                if (sessionId != null) {
                    countedSessionIds.add(sessionId);
                }
                if (session.getMessages() != null) {
                    messageCount += session.getMessages().size();
                }
            }

            Map<String, Object> stats = new HashMap<>();
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
