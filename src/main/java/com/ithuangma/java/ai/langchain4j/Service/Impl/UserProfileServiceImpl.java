
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
     * 计算相似用户
     */
    private List<UserProfile> calculateSimilarUsers(Long userId, int limit) {
        System.out.println("=== 开始查找相似用户 ===");
        System.out.println("查询用户ID: " + userId);
        
        UserProfile userProfile = getUserProfileByUserId(userId);
        if (userProfile == null) {
            System.out.println("警告: 用户画像不存在, userId=" + userId);
            return new java.util.ArrayList<>(); // 返回可序列化的空列表
        }
        System.out.println("用户画像获取成功");

        // 构建用户画像描述用于向量化
        String userProfileDescription = buildUserProfileDescription(userProfile);
        System.out.println("用户画像描述: " + userProfileDescription);

        // 生成嵌入向量
        System.out.println("正在生成embedding...");
        Embedding queryEmbedding = embeddingModel.embed(userProfileDescription).content();
        System.out.println("embedding生成完成");

        // 在向量数据库中搜索相似用户
        System.out.println("正在执行向量搜索...");
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(limit)
                .filter(MetadataFilterBuilder.metadataKey("type").isEqualTo("user")) // 只搜索用户
                .build();
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();
        
        System.out.println("向量搜索结果数量: " + matches.size());
        for (int i = 0; i < matches.size(); i++) {
            EmbeddingMatch<TextSegment> match = matches.get(i);
            TextSegment embedded = match.embedded();
            if (embedded != null) {
                System.out.println("匹配" + (i+1) + " - 相似度: " + match.score());
                System.out.println("   -> Metadata 原始内容: " + embedded.metadata().toMap());
                String matchedUserId = embedded.metadata().getString("user_id");
                System.out.println("   -> 提取到的 user_id: " + matchedUserId);
            } else {
                System.out.println("匹配" + (i+1) + " - embedded 为 null");
            }
        }

        // 提取匹配的用户ID
        List<Long> matchedUserIds = matches.stream()
                .map(match -> {
                    String userIdStr = match.embedded().metadata().getString("user_id");
                    try {
                        return Long.parseLong(userIdStr);
                    } catch (NumberFormatException e) {
                        System.err.println("无法解析user_id: " + userIdStr);
                        return null;
                    }
                })
                .filter(id -> id != null && !id.equals(userId)) // 排除自己
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());

        System.out.println("匹配到的用户ID列表: " + matchedUserIds);

        // 根据匹配的用户ID获取用户画像
        List<UserProfile> result = getUserProfilesByUserIds(matchedUserIds);
        System.out.println("最终返回的相似用户数量: " + (result != null ? result.size() : 0));
        System.out.println("=== 查找相似用户完成 ===");
        return result;
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
     * 计算队伍推荐
     */
    private List<Long> calculateTeamRecommendations(Long userId) {
        long startTime = System.currentTimeMillis();
        System.out.println("[性能监控] 开始查找适合队伍, userId=" + userId);

        UserProfile userProfile = getUserProfileByUserId(userId);
        if (userProfile == null) {
            return List.of();
        }
        System.out.println("[性能监控] 查询用户画像完成, 耗时: " + (System.currentTimeMillis() - startTime) + "ms");

        // 构建用户画像描述用于向量化
        String userProfileDescription = buildUserProfileDescription(userProfile);

        // 生成嵌入向量
        long embedStartTime = System.currentTimeMillis();
        Embedding queryEmbedding = embeddingModel.embed(userProfileDescription).content();
        System.out.println("[性能监控] 生成embedding完成, 耗时: " + (System.currentTimeMillis() - embedStartTime) + "ms");

        // 在向量数据库中搜索匹配的队伍
        long searchStartTime = System.currentTimeMillis();
        System.out.println("[性能监控] 开始向量搜索...");

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(5)
                .minScore(0.5)  // 设置最小相似度阈值
                .filter(MetadataFilterBuilder.metadataKey("type").isEqualTo("team")) // 关键：只搜队伍
                .build();
        System.out.println("[性能监控] 搜索请求已构建，开始执行搜索...");

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        System.out.println("[性能监控] 搜索执行完成，正在获取结果...");

        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();
        System.out.println("[性能监控] 向量搜索完成, 耗时: " + (System.currentTimeMillis() - searchStartTime) + "ms, 结果数: " + matches.size());

        System.out.println("=== 队伍推荐调试信息 ===");
        System.out.println("用户ID: " + userId);
        System.out.println("向量搜索结果数量: " + matches.size());
        for (int i = 0; i < matches.size(); i++) {
            EmbeddingMatch<TextSegment> match = matches.get(i);
            TextSegment embedded = match.embedded();
            if (embedded != null) {
                // 打印完整的 Metadata 对象，看看里面到底有什么
                System.out.println("匹配" + (i+1) + " - 相似度: " + match.score());
                System.out.println("   -> Metadata 原始内容: " + embedded.metadata().toMap());
                String teamIdStr = embedded.metadata().getString("teamId");
                System.out.println("   -> 提取到的 teamId: " + teamIdStr);
            } else {
                System.out.println("匹配" + (i+1) + " - embedded 为 null");
            }
        }
        System.out.println("========================");

        // 提取匹配的队伍ID，并进行null检查
        List<Long> matchedTeamIds = matches.stream()
                .map(match -> {
                    TextSegment embedded = match.embedded();
                    if (embedded == null) {
                        return null;
                    }
                    // 统一使用 "teamId" 作为元数据键名
                    String teamIdStr = embedded.metadata().getString("teamId");
                    if (teamIdStr == null || teamIdStr.isEmpty()) {
                        return null;
                    }
                    try {
                        return Long.parseLong(teamIdStr);
                    } catch (NumberFormatException e) {
                        System.err.println("无法解析teamId: " + teamIdStr);
                        return null;
                    }
                })
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        System.out.println("最终推荐的队伍ID列表: " + matchedTeamIds);

        // 验证队伍ID是否在数据库中存在
        List<Long> validTeamIds = matchedTeamIds.stream()
                .filter(teamId -> {
                    TeamProfile team = teamProfileMapper.selectById(teamId);
                    boolean exists = team != null;
                    if (!exists) {
                        System.out.println("警告: 队伍ID " + teamId + " 在数据库中不存在，已过滤");
                    } else {
                        System.out.println("确认: 队伍ID " + teamId + " (" + team.getName() + ") 存在");
                    }
                    return exists;
                })
                .collect(Collectors.toList());

        System.out.println("验证后的有效队伍ID列表: " + validTeamIds);
        System.out.println("[性能监控] 总耗时: " + (System.currentTimeMillis() - startTime) + "ms");
        return validTeamIds;
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

            // 创建文本段落
            TextSegment textSegment = TextSegment.from(userProfileDescription);

            // 添加元数据
            textSegment.metadata().put("user_id", userProfile.getUserId().toString());
            textSegment.metadata().put("mbti_type", userProfile.getMbtiType() != null ? userProfile.getMbtiType() : "");
            textSegment.metadata().put("tags", userProfile.getTags() != null ? userProfile.getTags() : "");
            textSegment.metadata().put("type", "user"); // 标识这是用户数据

            // 生成嵌入向量
            Embedding embedding = embeddingModel.embed(textSegment).content();

            // 存储到Pinecone
            embeddingStore.add(embedding, textSegment);
        } catch (Exception e) {
            e.printStackTrace();
            // 记录错误日志，但不影响主流程
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