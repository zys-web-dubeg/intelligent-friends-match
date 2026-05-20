
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
import com.ithuangma.java.ai.langchain4j.matching.MatchResult;
import com.ithuangma.java.ai.langchain4j.matching.MultiDimensionMatcher;
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

    @Autowired
    private MultiDimensionMatcher multiDimensionMatcher;

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
        // 第1检（无锁）：快速失败，避免不必要的锁竞争
        if (isMemberOfTeam(teamId, userId)) {
            throw new BusinessException("用户已加入队伍", ErrorCode.PARAMS_ERROR.getCode(), "用户已加入队伍");
        }

        RLock lock = redissonClient.getLock("team_join_lock:" + teamId + ":" + userId);
        try {
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);

            if (acquired) {
                // 第2检（有锁）：防止第1检与加锁之间被其他线程插入
                if (isMemberOfTeam(teamId, userId)) {
                    throw new BusinessException("用户已加入队伍", ErrorCode.PARAMS_ERROR.getCode(), "用户已加入队伍");
                }

                TeamProfile teamProfile = teamProfileMapper.selectById(teamId);
                if (teamProfile == null) {
                    throw new BusinessException("队伍不存在", ErrorCode.PARAMS_ERROR.getCode(), "队伍不存在");
                }

                if (teamProfile.getAccessLevel() == 0) {
                    TeamMemberRelation relation = TeamMemberRelation.builder()
                            .teamId(teamId).userId(userId).role("member").build();
                    teamMemberRelationMapper.insert(relation);
                    return true;
                } else if (teamProfile.getAccessLevel() == 1) {
                    boolean isMatched = checkCompatibility(teamId, userId);
                    if (isMatched) {
                        TeamMemberRelation relation = TeamMemberRelation.builder()
                                .teamId(teamId).userId(userId).role("member").build();
                        teamMemberRelationMapper.insert(relation);
                        return true;
                    }
                    throw new BusinessException("用户未匹配成功", ErrorCode.PARAMS_ERROR.getCode(), "用户未匹配成功，综合评分低于0.6");
                } else {
                    throw new BusinessException("队伍为私有队伍", ErrorCode.PARAMS_ERROR.getCode(), "队伍为私有队伍，需要邀请才能加入");
                }
            } else {
                throw new BusinessException("操作过于频繁", ErrorCode.PARAMS_ERROR.getCode(), "操作过于频繁，请稍后再试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("操作被中断", ErrorCode.SYSTEM_ERROR.getCode(), "操作被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 检查用户是否已是队伍成员（抽取为独立方法，第1检和第2检共用）
     */
    private boolean isMemberOfTeam(Long teamId, Long userId) {
        QueryWrapper<TeamMemberRelation> wrapper = new QueryWrapper<>();
        wrapper.eq("team_id", teamId).eq("user_id", userId);
        return teamMemberRelationMapper.selectCount(wrapper) > 0;
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
     * 计算推荐队伍（多维加权评分）
     */
    private List<TeamProfile> calculateRecommendations(Long userId) {
        UserProfile userProfile = userProfileMapper.selectOne(
                new QueryWrapper<UserProfile>().eq("user_id", userId));

        if (userProfile == null) {
            return List.of();
        }

        try {
            // 1. 用完整画像做初始向量搜索，获取更多候选队伍
            String userQueryText = buildTeamMatchQueryText(userProfile);
            Embedding userEmbedding = embeddingModel.embed(userQueryText).content();

            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(userEmbedding)
                    .maxResults(15)        // 扩大候选池
                    .minScore(0.4)         // 降低阈值获取更多候选
                    .filter(MetadataFilterBuilder.metadataKey("type").isEqualTo("team"))
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

            System.out.println("=== TeamServiceImpl 多维匹配推荐 ===");
            System.out.println("候选队伍数: " + matches.size());

            // 2. 解析候选队伍，计算多维匹配分数
            List<ScoredTeam> scoredTeams = new java.util.ArrayList<>();

            for (EmbeddingMatch<TextSegment> match : matches) {
                TextSegment segment = match.embedded();
                if (segment == null) continue;

                String teamIdStr = segment.metadata().getString("teamId");
                if (teamIdStr == null) continue;

                try {
                    Long teamId = Long.parseLong(teamIdStr);
                    TeamProfile team = teamProfileMapper.selectById(teamId);
                    if (team == null) continue;

                    // 多维加权评分
                    MatchResult result = multiDimensionMatcher.scoreUserTeamMatch(userProfile, team);
                    scoredTeams.add(new ScoredTeam(team, result.getOverallScore(), result));

                } catch (NumberFormatException e) {
                    System.err.println("teamId 格式错误: " + teamIdStr);
                }
            }

            // 3. 按综合评分排序，取前5
            scoredTeams.sort((a, b) -> Double.compare(b.score, a.score));

            printRecommendationResult(scoredTeams);

            return scoredTeams.stream()
                    .limit(5)
                    .map(st -> st.team)
                    .toList();

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private void printRecommendationResult(List<ScoredTeam> scoredTeams) {
        System.out.println("==== 推荐排名 ====");
        for (int i = 0; i < Math.min(5, scoredTeams.size()); i++) {
            ScoredTeam st = scoredTeams.get(i);
            System.out.println("第" + (i + 1) + "名: " + st.team.getName()
                    + " | 综合分: " + String.format("%.2f", st.score));
            st.result.getDimensionScores().forEach(d ->
                    System.out.println("    " + d.getName() + ": " + String.format("%.2f", d.getScore())));
        }
        System.out.println("====================");
    }

    /** 构建用于队伍匹配的查询文本 */
    private String buildTeamMatchQueryText(UserProfile user) {
        StringBuilder sb = new StringBuilder();
        if (user.getTags() != null) sb.append("标签：").append(user.getTags()).append("\n");
        if (user.getInterests() != null) sb.append("兴趣爱好：").append(user.getInterests()).append("\n");
        if (user.getPreferences() != null) sb.append("偏好：").append(user.getPreferences()).append("\n");
        if (user.getPersonalityTraits() != null) sb.append("性格特征：").append(user.getPersonalityTraits()).append("\n");
        if (user.getCommunicationStyle() != null) sb.append("沟通风格：").append(user.getCommunicationStyle()).append("\n");
        return sb.toString();
    }

    /** 带评分的队伍候选 */
    private static class ScoredTeam {
        final TeamProfile team;
        final double score;
        final MatchResult result;

        ScoredTeam(TeamProfile team, double score, MatchResult result) {
            this.team = team;
            this.score = score;
            this.result = result;
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

    // 检查用户与队伍的兼容性（使用多维加权评分）
    private boolean checkCompatibility(Long teamId, Long userId) {
        UserProfile userProfile = userProfileMapper.selectOne(
                new QueryWrapper<UserProfile>().eq("user_id", userId));

        TeamProfile teamProfile = teamProfileMapper.selectById(teamId);

        if (userProfile == null || teamProfile == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户或队伍不存在");
        }

        if (teamProfile.getPersonalityConstraint() == null || teamProfile.getPersonalityConstraint().isBlank()) {
            return true; // 队伍没有约束条件，默认允许加入
        }

        try {
            MatchResult result = multiDimensionMatcher.scoreUserTeamMatch(userProfile, teamProfile);
            System.out.println("checkCompatibility 用户-" + userId + " 队伍-" + teamId
                    + " 综合分: " + String.format("%.2f", result.getOverallScore()));
            result.getDimensionScores().forEach(d ->
                    System.out.println("  - " + d.getName() + ": " + String.format("%.2f", d.getScore())
                            + " (权重 " + String.format("%.2f", d.getWeight()) + ")"));

            return result.getOverallScore() >= 0.6;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
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
}
