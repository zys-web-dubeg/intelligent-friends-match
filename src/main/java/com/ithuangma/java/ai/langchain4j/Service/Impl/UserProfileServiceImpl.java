
package com.ithuangma.java.ai.langchain4j.Service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ithuangma.java.ai.langchain4j.entity.TeamProfile;
import com.ithuangma.java.ai.langchain4j.entity.UserProfile;
import com.ithuangma.java.ai.langchain4j.mapper.TeamProfileMapper;
import com.ithuangma.java.ai.langchain4j.mapper.UserProfileMapper;
import com.ithuangma.java.ai.langchain4j.Service.UserProfileService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.EmbeddingStore;
import com.ithuangma.java.ai.langchain4j.matching.MatchResult;
import com.ithuangma.java.ai.langchain4j.matching.MultiDimensionMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private TeamProfileMapper teamProfileMapper;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MultiDimensionMatcher multiDimensionMatcher;

    private static final String USER_PROFILE_TEAM_RECOMMENDATION_CACHE_PREFIX = "user:profile:team:recommendation:user:";
    private static final String USER_SIMILARITY_CACHE_PREFIX = "user:similarity:users:user:";
    private static final int CACHE_TTL_MINUTES = 30;

    // 保存用户画像
    @Override
    public UserProfile saveUserProfile(UserProfile userProfile) {
        // 检查用户画像是否已存在
        QueryWrapper<UserProfile> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userProfile.getUserId());
        UserProfile existingProfile = userProfileMapper.selectOne(wrapper);

        if (existingProfile != null) {
            // 更新现有记录
            userProfile.setId(existingProfile.getId());
            userProfileMapper.updateById(userProfile);
        } else {
            // 插入新记录
            userProfileMapper.insert(userProfile);
        }

        // 将用户画像信息向量化并存储到Pinecone
        storeUserProfileInPinecone(userProfile);

        return userProfile;
    }

    // 根据用户ID获取用户画像
    @Override
    public UserProfile getUserProfileByUserId(Long userId) {
        QueryWrapper<UserProfile> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return userProfileMapper.selectOne(wrapper);
    }

    // 根据用户ID列表批量获取用户画像
    @Override
    public List<UserProfile> getUserProfilesByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        QueryWrapper<UserProfile> wrapper = new QueryWrapper<>();
        wrapper.in("user_id", userIds);
        return userProfileMapper.selectList(wrapper);
    }

    // 更新用户标签
    @Override
    public UserProfile updateUserTags(Long userId, String tags) {
        UserProfile profile = getUserProfileByUserId(userId);
        if (profile == null) {
            profile = UserProfile.builder()
                    .userId(userId)
                    .tags(tags)
                    .build();
        } else {
            profile.setTags(tags);
        }

        return saveUserProfile(profile);
    }

    // 更新MBTI类型
    @Override
    public UserProfile updateMbtiType(Long userId, String mbtiType) {
        UserProfile profile = getUserProfileByUserId(userId);
        if (profile == null) {
            profile = UserProfile.builder()
                    .userId(userId)
                    .mbtiType(mbtiType)
                    .build();
        } else {
            profile.setMbtiType(mbtiType);
        }

        return saveUserProfile(profile);
    }

    // 查找相似用户
    @Override
    public List<UserProfile> findSimilarUsers(Long userId, int limit) {
        String cacheKey = USER_SIMILARITY_CACHE_PREFIX + userId + ":limit:" + limit;

        // 尝试从缓存获取相似用户结果
        try {
            List<UserProfile> cachedSimilarUsers = (List<UserProfile>) redisTemplate.opsForValue().get(cacheKey);

            if (cachedSimilarUsers != null) {
                // 缓存命中，异步更新缓存
                updateSimilarUsersAsync(userId, limit, cacheKey);
                return cachedSimilarUsers;
            }
        } catch (Exception e) {
            // 缓存反序列化失败，删除损坏的缓存
            System.err.println("缓存反序列化失败，删除损坏的缓存: " + cacheKey + ", 错误: " + e.getMessage());
            redisTemplate.delete(cacheKey);
        }

        // 缓存未命中或已删除，计算相似用户结果
        List<UserProfile> similarUsers = calculateSimilarUsers(userId, limit);

        // 将结果存入缓存
        redisTemplate.opsForValue().set(cacheKey, similarUsers,
                Duration.ofMinutes(CACHE_TTL_MINUTES));

        return similarUsers;
    }

    /**
     * 计算相似用户（多维加权评分）
     */
    private List<UserProfile> calculateSimilarUsers(Long userId, int limit) {
        System.out.println("=== 开始查找相似用户 (多维评分) ===");
        System.out.println("查询用户ID: " + userId);

        UserProfile userProfile = getUserProfileByUserId(userId);
        if (userProfile == null) {
            System.out.println("警告: 用户画像不存在, userId=" + userId);
            return new java.util.ArrayList<>();
        }

        // 构建用户画像描述用于向量化
        String userProfileDescription = buildUserProfileDescription(userProfile);
        Embedding queryEmbedding = embeddingModel.embed(userProfileDescription).content();

        // 在向量数据库中搜索相似用户，获取更多候选用于多维重排
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(limit * 3)  // 获取更多候选
                .filter(MetadataFilterBuilder.metadataKey("type").isEqualTo("user"))
                .build();
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

        System.out.println("候选用户数: " + matches.size());

        // 提取候选用户ID
        List<Long> candidateIds = matches.stream()
                .map(match -> {
                    TextSegment embedded = match.embedded();
                    if (embedded == null) return null;
                    String uid = embedded.metadata().getString("user_id");
                    if (uid == null || uid.isBlank()) return null;
                    try { return Long.parseLong(uid); }
                    catch (NumberFormatException e) { return null; }
                })
                .filter(id -> id != null && !id.equals(userId))
                .distinct()
                .collect(Collectors.toList());

        if (candidateIds.isEmpty()) return new java.util.ArrayList<>();

        List<UserProfile> candidates = getUserProfilesByUserIds(candidateIds);

        // 多维评分 + 排序
        List<ScoredUser> scoredUsers = new java.util.ArrayList<>();
        for (UserProfile candidate : candidates) {
            MatchResult result = multiDimensionMatcher.scoreUserSimilarity(userProfile, candidate);
            scoredUsers.add(new ScoredUser(candidate, result.getOverallScore(), result));
        }

        scoredUsers.sort((a, b) -> Double.compare(b.score, a.score));

        System.out.println("==== 相似用户排名 ====");
        for (int i = 0; i < Math.min(limit, scoredUsers.size()); i++) {
            ScoredUser su = scoredUsers.get(i);
            System.out.println("第" + (i + 1) + "名: userId=" + su.profile.getUserId()
                    + " | 综合分: " + String.format("%.2f", su.score));
            su.result.getDimensionScores().forEach(d ->
                    System.out.println("    " + d.getName() + ": " + String.format("%.2f", d.getScore())));
        }
        System.out.println("======================");

        return scoredUsers.stream()
                .limit(limit)
                .map(su -> su.profile)
                .collect(Collectors.toList());
    }

    /**
     * 异步更新相似用户缓存
     */
    private void updateSimilarUsersAsync(Long userId, int limit, String cacheKey) {
        // 使用 CompletableFuture 实现异步更新缓存，避免阻塞当前请求
        CompletableFuture.runAsync(() -> {
            try {
                List<UserProfile> freshSimilarUsers = calculateSimilarUsers(userId, limit);
                redisTemplate.opsForValue().set(cacheKey, freshSimilarUsers,
                        Duration.ofMinutes(CACHE_TTL_MINUTES));
            } catch (Exception e) {
                System.err.println("Failed to update similar users cache for user: " + userId + ", error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // 构建用户画像描述
    @Override
    public List<Long> findSuitableTeamsByProfile(Long userId) {
        String cacheKey = USER_PROFILE_TEAM_RECOMMENDATION_CACHE_PREFIX + userId;

        // 尝试从缓存获取推荐结果
        try {
            List<Long> cachedRecommendations = (List<Long>) redisTemplate.opsForValue().get(cacheKey);

            if (cachedRecommendations != null) {
                // 缓存命中，异步更新缓存
                updateTeamRecommendationsAsync(userId, cacheKey);
                return cachedRecommendations;
            }
        } catch (Exception e) {
            // 缓存反序列化失败，删除损坏的缓存
            System.err.println("缓存反序列化失败，删除损坏的缓存: " + cacheKey + ", 错误: " + e.getMessage());
            redisTemplate.delete(cacheKey);
        }

        // 缓存未命中或已删除，计算推荐结果
        List<Long> recommendations = calculateTeamRecommendations(userId);

        // 将结果存入缓存
        redisTemplate.opsForValue().set(cacheKey, recommendations,
                Duration.ofMinutes(CACHE_TTL_MINUTES));

        return recommendations;
    }

    /**
     * 计算队伍推荐（多维加权评分）
     */
    private List<Long> calculateTeamRecommendations(Long userId) {
        long startTime = System.currentTimeMillis();
        System.out.println("[性能监控] 开始查找适合队伍 (多维评分), userId=" + userId);

        UserProfile userProfile = getUserProfileByUserId(userId);
        if (userProfile == null) return List.of();

        String userProfileDescription = buildUserProfileDescription(userProfile);
        Embedding queryEmbedding = embeddingModel.embed(userProfileDescription).content();

        // 搜索候选队伍，扩大候选池
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(15)
                .minScore(0.4)
                .filter(MetadataFilterBuilder.metadataKey("type").isEqualTo("team"))
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();
        System.out.println("[性能监控] 获取" + matches.size() + "个候选队伍, 耗时: "
                + (System.currentTimeMillis() - startTime) + "ms");

        // 多维评分排序
        List<ScoredTeam> scoredTeams = new java.util.ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment embedded = match.embedded();
            if (embedded == null) continue;

            String teamIdStr = embedded.metadata().getString("teamId");
            if (teamIdStr == null || teamIdStr.isBlank()) continue;

            try {
                Long teamId = Long.parseLong(teamIdStr);
                TeamProfile team = teamProfileMapper.selectById(teamId);
                if (team == null) continue;

                MatchResult result = multiDimensionMatcher.scoreUserTeamMatch(userProfile, team);
                scoredTeams.add(new ScoredTeam(teamId, result.getOverallScore(), result));

            } catch (NumberFormatException e) {
                System.err.println("无法解析teamId: " + teamIdStr);
            }
        }

        scoredTeams.sort((a, b) -> Double.compare(b.score, a.score));

        System.out.println("==== 队伍推荐 (多维评分) ====");
        for (int i = 0; i < Math.min(5, scoredTeams.size()); i++) {
            ScoredTeam st = scoredTeams.get(i);
            System.out.println("第" + (i + 1) + "名: teamId=" + st.teamId
                    + " | 综合分: " + String.format("%.2f", st.score));
            st.result.getDimensionScores().forEach(d ->
                    System.out.println("    " + d.getName() + ": " + String.format("%.2f", d.getScore())));
        }
        System.out.println("[性能监控] 总耗时: " + (System.currentTimeMillis() - startTime) + "ms");

        return scoredTeams.stream()
                .limit(5)
                .map(st -> st.teamId)
                .collect(Collectors.toList());
    }

    /**
     * 异步更新队伍推荐缓存
     */
    private void updateTeamRecommendationsAsync(Long userId, String cacheKey) {
        // 使用 CompletableFuture 实现异步更新缓存，避免阻塞当前请求
        CompletableFuture.runAsync(() -> {
            try {
                List<Long> freshRecommendations = calculateTeamRecommendations(userId);
                redisTemplate.opsForValue().set(cacheKey, freshRecommendations,
                        Duration.ofMinutes(CACHE_TTL_MINUTES));
            } catch (Exception e) {
                System.err.println("Failed to update team recommendation cache for user: " + userId + ", error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    // 将用户画像信息存储到Pinecone向量数据库
    private void storeUserProfileInPinecone(UserProfile userProfile) {
        try {
            String userProfileDescription = buildUserProfileDescription(userProfile);

            // 创建元数据
            dev.langchain4j.data.document.Metadata metadata = new dev.langchain4j.data.document.Metadata();
            metadata.put("user_id", userProfile.getUserId().toString());
            metadata.put("type", "user"); // 标识这是用户数据

            // 创建 TextSegment（将文本和元数据绑定）
            TextSegment textSegment = TextSegment.from(userProfileDescription, metadata);

            // 生成嵌入向量
            Embedding embedding = embeddingModel.embed(textSegment).content();

            // 存储到Pinecone
            embeddingStore.add(embedding, textSegment);
        } catch (Exception e) {
            e.printStackTrace();
            // 记录错误日志，但不影响主流程
        }
    }

    // ==================== 多维评分辅助类 ====================

    private static class ScoredUser {
        final UserProfile profile;
        final double score;
        final MatchResult result;

        ScoredUser(UserProfile profile, double score, MatchResult result) {
            this.profile = profile;
            this.score = score;
            this.result = result;
        }
    }

    private static class ScoredTeam {
        final Long teamId;
        final double score;
        final MatchResult result;

        ScoredTeam(Long teamId, double score, MatchResult result) {
            this.teamId = teamId;
            this.score = score;
            this.result = result;
        }
    }

    // 构建用户画像描述字符串
    private String buildUserProfileDescription(UserProfile userProfile) {
        StringBuilder sb = new StringBuilder();

        sb.append("用户画像信息：\n");
        if (userProfile.getMbtiType() != null) {
            sb.append("MBTI类型：").append(userProfile.getMbtiType()).append("\n");
        }
        if (userProfile.getTags() != null) {
            sb.append("标签：").append(userProfile.getTags()).append("\n");
        }
        if (userProfile.getInterests() != null) {
            sb.append("兴趣爱好：").append(userProfile.getInterests()).append("\n");
        }
        if (userProfile.getPreferences() != null) {
            sb.append("偏好设置：").append(userProfile.getPreferences()).append("\n");
        }
        if (userProfile.getPersonalityTraits() != null) {
            sb.append("性格特征：").append(userProfile.getPersonalityTraits()).append("\n");
        }
        if (userProfile.getCommunicationStyle() != null) {
            sb.append("沟通风格：").append(userProfile.getCommunicationStyle()).append("\n");
        }

        return sb.toString();
    }
}