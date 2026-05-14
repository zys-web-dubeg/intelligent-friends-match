
package com.ithuangma.java.ai.langchain4j.Service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ithuangma.java.ai.langchain4j.common.ErrorCode;
import com.ithuangma.java.ai.langchain4j.entity.TeamProfile;
import com.ithuangma.java.ai.langchain4j.entity.TeamMemberRelation;
import com.ithuangma.java.ai.langchain4j.entity.UserProfile;
import com.ithuangma.java.ai.langchain4j.exception.BusinessException;
import com.ithuangma.java.ai.langchain4j.mapper.TeamProfileMapper;
import com.ithuangma.java.ai.langchain4j.mapper.TeamMemberRelationMapper;
import com.ithuangma.java.ai.langchain4j.mapper.UserProfileMapper;
import com.ithuangma.java.ai.langchain4j.Service.TeamService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.model.embedding.onnx.HuggingFaceTokenizer;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class TeamServiceImpl implements TeamService {

    @Autowired
    private TeamProfileMapper teamProfileMapper;

    @Autowired
    private TeamMemberRelationMapper teamMemberRelationMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    private static final String TEAM_RECOMMENDATION_CACHE_PREFIX = "team:recommendation:user:";
    private static final int CACHE_TTL_MINUTES = 30;

    @Override
    public TeamProfile createTeam(TeamProfile teamProfile) {
        // 保存队伍信息
        teamProfileMapper.insert(teamProfile);

        // 将队伍特征转换为向量并存储到向量数据库
        if (teamProfile.getPersonalityConstraint() != null && !teamProfile.getPersonalityConstraint().isEmpty()) {
            indexTeamVector(teamProfile);
        }

        return teamProfile;
    }

    @Override
    public List<TeamProfile> getAllTeams() {
        return teamProfileMapper.selectList(null);
    }

    @Override
    public TeamProfile getTeamById(Long teamId) {
        return teamProfileMapper.selectById(teamId);
    }

    @Override
    public boolean joinTeam(Long teamId, Long userId) {
        // 使用Redisson分布式锁防止重复加入
        RLock lock = redissonClient.getLock("team_join_lock:" + teamId + ":" + userId);

        try {
            // 尝试获取锁，等待时间为5秒，锁自动释放时间为10秒
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);

            if (acquired) {
                // 再次检查用户是否已经在这个队伍中
                QueryWrapper<TeamMemberRelation> wrapper = new QueryWrapper<>();
                wrapper.eq("team_id", teamId).eq("user_id", userId);
                if (teamMemberRelationMapper.selectCount(wrapper) > 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入队伍"); // 已经在队伍中
                }

                // 检查队伍是否存在
                TeamProfile teamProfile = teamProfileMapper.selectById(teamId);
                if (teamProfile == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
                }

                // 根据队伍的访问级别进行不同处理
                if (teamProfile.getAccessLevel() == 0) { // 公开队伍
                    // 直接加入
                    TeamMemberRelation relation = TeamMemberRelation.builder()
                            .teamId(teamId)
                            .userId(userId)
                            .role("member")
                            .build();
                    teamMemberRelationMapper.insert(relation);
                    return true;
                } else if (teamProfile.getAccessLevel() == 1) { // 需要匹配的队伍
                    // 执行匹配算法
                    boolean isMatched = checkCompatibility(teamId, userId);
                    if (isMatched) {
                        TeamMemberRelation relation = TeamMemberRelation.builder()
                                .teamId(teamId)
                                .userId(userId)
                                .role("member")
                                .build();
                        teamMemberRelationMapper.insert(relation);
                        return true;
                    }
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户未匹配成功");
                } else { // 私有队伍
                    // 只有邀请才能加入
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍为私有队伍，需要邀请才能加入");
                }
            } else {
                // 获取锁失败，说明已经有其他请求正在处理该用户的入队操作
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "操作过于频繁，请稍后再试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "操作被中断");
        } finally {
            // 释放分布式锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public List<TeamProfile> getUserTeams(Long userId) {
        // 获取用户加入的所有队伍ID
        QueryWrapper<TeamMemberRelation> wrapper = new QueryWrapper<>();
        wrapper.select("team_id").eq("user_id", userId);
        List<TeamMemberRelation> relations = teamMemberRelationMapper.selectList(wrapper);

        // 根据队伍ID获取队伍信息
        if (relations.isEmpty()) {
            return List.of();
        }

        List<Long> teamIds = relations.stream()
                .map(TeamMemberRelation::getTeamId)
                .toList();

        return teamProfileMapper.selectBatchIds(teamIds);
    }

    @Override
    public List<TeamProfile> matchTeamsForUser(Long userId) {
        String cacheKey = TEAM_RECOMMENDATION_CACHE_PREFIX + userId;

        // 尝试从缓存获取推荐结果
        try {
            List<TeamProfile> cachedRecommendations = (List<TeamProfile>) redisTemplate.opsForValue().get(cacheKey);

            if (cachedRecommendations != null) {
                // 缓存命中，异步更新缓存
                updateRecommendationsAsync(userId, cacheKey);
                return cachedRecommendations;
            }
        } catch (Exception e) {
            // 缓存反序列化失败，删除损坏的缓存
            System.err.println("缓存反序列化失败，删除损坏的缓存: " + cacheKey + ", 错误: " + e.getMessage());
            redisTemplate.delete(cacheKey);
        }

        // 缓存未命中或已删除，计算推荐结果
        List<TeamProfile> recommendations = calculateRecommendations(userId);

        // 将结果存入缓存
        redisTemplate.opsForValue().set(cacheKey, recommendations,
                Duration.ofMinutes(CACHE_TTL_MINUTES));

        return recommendations;
    }

    /**
     * 计算推荐队伍
     */
    private List<TeamProfile> calculateRecommendations(Long userId) {
        // 获取用户画像
        UserProfile userProfile = userProfileMapper.selectOne(
                new QueryWrapper<UserProfile>().eq("user_id", userId));

        if (userProfile == null || userProfile.getTags() == null || userProfile.getTags().isEmpty()) {
            return List.of();
        }

        // 将用户画像转换为向量
        try {
            Embedding userEmbedding = embeddingModel.embed(userProfile.getTags()).content();

            // 创建搜索请求，增加过滤条件：只搜索 type 为 team 的数据
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(userEmbedding)
                    .maxResults(5)
                    .minScore(0.6)
                    .filter(MetadataFilterBuilder.metadataKey("type").isEqualTo("team"))
                    .build();

            // 在向量数据库中搜索最相似的队伍
            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

            System.out.println("=== TeamServiceImpl 调试信息 ===");
            System.out.println("搜索结果数量: " + matches.size());

            // 根据匹配结果返回对应的队伍
            return matches.stream()
                    .map(match -> {
                        TextSegment segment = match.embedded();
                        if (segment == null) {
                            System.out.println("警告: 匹配项的 embedded 为 null");
                            return null;
                        }

                        // 打印所有可用的 Metadata 键名，方便排查
                        System.out.println("Metadata 键名列表: " + segment.metadata().toMap().keySet());

                        String teamIdStr = segment.metadata().getString("teamId");
                        System.out.println("提取到的 teamId: " + teamIdStr);

                        if (teamIdStr != null) {
                            try {
                                Long teamId = Long.parseLong(teamIdStr);
                                return teamProfileMapper.selectById(teamId);
                            } catch (NumberFormatException e) {
                                System.err.println("teamId 格式错误: " + teamIdStr);
                                return null;
                            }
                        }
                        return null;
                    })
                    .filter(team -> team != null)
                    .toList();
        } catch (Exception e) {
            e.printStackTrace();
            // 如果向量匹配失败，返回空列表
            return List.of();
        }
    }

    /**
     * 异步更新推荐缓存
     */
    private void updateRecommendationsAsync(Long userId, String cacheKey) {
        // 使用异步任务更新缓存，避免阻塞当前请求
        CompletableFuture.runAsync(() -> {
            try {
                List<TeamProfile> freshRecommendations = calculateRecommendations(userId);
                redisTemplate.opsForValue().set(cacheKey, freshRecommendations,
                        Duration.ofMinutes(CACHE_TTL_MINUTES));
            } catch (Exception e) {
                System.err.println("Failed to update recommendation cache for user: " + userId + ", error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public List<TeamMemberRelation> getTeamMembers(Long teamId) {
        QueryWrapper<TeamMemberRelation> wrapper = new QueryWrapper<>();
        wrapper.eq("team_id", teamId);
        return teamMemberRelationMapper.selectList(wrapper);
    }

    // 检查用户与队伍的兼容性
    private boolean checkCompatibility(Long teamId, Long userId) {
        // 获取用户画像
        UserProfile userProfile = userProfileMapper.selectOne(
                new QueryWrapper<UserProfile>().eq("user_id", userId));

        // 获取队伍信息
        TeamProfile teamProfile = teamProfileMapper.selectById(teamId);

        if (userProfile == null || teamProfile == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户或队伍不存在");
        }

        // 使用向量相似度进行匹配
        try {
            String userDescription = userProfile.getTags();
            String teamConstraint = teamProfile.getPersonalityConstraint();

            if (userDescription == null || teamConstraint == null) {
                return true; // 默认允许加入
            }

            // 将用户和队伍转换为向量
            Embedding userEmbedding = embeddingModel.embed(userDescription).content();
            Embedding teamEmbedding = embeddingModel.embed(teamConstraint).content();

            // 计算余弦相似度
            double similarity = cosineSimilarity(userEmbedding.vectorAsList(), teamEmbedding.vectorAsList());

            // 相似度阈值设为 0.6
            return similarity >= 0.6;
        } catch (Exception e) {
            e.printStackTrace();
            return true; // 出错时默认允许
        }
    }

    /**
     * 将队伍特征索引到向量数据库
     */
    private void indexTeamVector(TeamProfile teamProfile) {
        try {
            // 1. 构建元数据，必须包含 type 标记以便过滤
            Metadata metadata = new Metadata();
            metadata.put("teamId", teamProfile.getId().toString());
            metadata.put("type", "team"); // 关键：与初始化器保持一致

            // 2. 创建文本段
            TextSegment textSegment = TextSegment.from(
                    teamProfile.getPersonalityConstraint(),
                    metadata
            );

            // 3. 生成向量并存储
            Embedding embedding = embeddingModel.embed(textSegment).content();
            embeddingStore.add(embedding, textSegment);

            System.out.println("队伍 [" + teamProfile.getName() + "] 的向量已索引 (ID: " + teamProfile.getId() + ")");
        } catch (Exception e) {
            System.err.println("队伍 " + teamProfile.getId() + " 的向量索引失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 计算两个向量的余弦相似度
     */
    private double cosineSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1.size() != vector2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            float v1 = vector1.get(i);
            float v2 = vector2.get(i);

            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
