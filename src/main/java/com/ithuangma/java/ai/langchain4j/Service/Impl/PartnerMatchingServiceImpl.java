package com.ithuangma.java.ai.langchain4j.Service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ithuangma.java.ai.langchain4j.Service.PartnerMatchingService;
import com.ithuangma.java.ai.langchain4j.common.ErrorCode;
import com.ithuangma.java.ai.langchain4j.entity.PairRelation;
import com.ithuangma.java.ai.langchain4j.entity.UserProfile;
import com.ithuangma.java.ai.langchain4j.exception.BusinessException;
import com.ithuangma.java.ai.langchain4j.mapper.PairRelationMapper;
import com.ithuangma.java.ai.langchain4j.mapper.UserProfileMapper;
import com.ithuangma.java.ai.langchain4j.matching.MatchResult;
import com.ithuangma.java.ai.langchain4j.matching.MultiDimensionMatcher;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PartnerMatchingServiceImpl implements PartnerMatchingService {

    @Autowired
    private PairRelationMapper pairRelationMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private MultiDimensionMatcher multiDimensionMatcher;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Override
    public List<PartnerCandidate> findPotentialPartners(Long userId, String matchType, int limit) {
        UserProfile myProfile = userProfileMapper.selectOne(
                new QueryWrapper<UserProfile>().eq("user_id", userId));

        if (myProfile == null) {
            return List.of();
        }

        // 1. 获取已有关联的用户ID（排除已有关系的用户）
        Set<Long> excludedUserIds = getRelatedUserIds(userId);

        // 2. 用向量搜索获取候选（快速预筛）
        String profileDesc = buildPartnerQueryText(myProfile);
        Embedding queryEmbedding = embeddingModel.embed(profileDesc).content();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(limit * 3)
                .filter(MetadataFilterBuilder.metadataKey("type").isEqualTo("user"))
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

        // 3. 提取候选用户ID
        List<Long> candidateIds = matches.stream()
                .map(m -> {
                    TextSegment seg = m.embedded();
                    if (seg == null) return null;
                    String uid = seg.metadata().getString("user_id");
                    if (uid == null || uid.isBlank()) return null;
                    try { return Long.parseLong(uid); }
                    catch (NumberFormatException e) { return null; }
                })
                .filter(id -> id != null && !id.equals(userId) && !excludedUserIds.contains(id))
                .distinct()
                .collect(Collectors.toList());

        if (candidateIds.isEmpty()) return List.of();

        // 4. 获取完整用户画像，多维评分排序
        List<UserProfile> candidates = userProfileMapper.selectList(
                new QueryWrapper<UserProfile>().in("user_id", candidateIds));

        List<PartnerCandidate> scored = new ArrayList<>();
        for (UserProfile candidate : candidates) {
            MatchResult result = multiDimensionMatcher.scorePartnerMatch(myProfile, candidate, matchType);
            Map<String, Double> dimMap = new LinkedHashMap<>();
            result.getDimensionScores().forEach(d -> dimMap.put(d.getName(), d.getScore()));

            scored.add(new PartnerCandidate(candidate, result.getOverallScore(), dimMap));
        }

        scored.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));

        // 打印推荐日志
        System.out.println("=== 伙伴推荐 (" + matchType + ") 用户-" + userId + " ===");
        for (int i = 0; i < Math.min(limit, scored.size()); i++) {
            PartnerCandidate pc = scored.get(i);
            System.out.println("第" + (i + 1) + "名: userId=" + pc.getUserProfile().getUserId()
                    + " | 综合分: " + String.format("%.2f", pc.getMatchScore()));
            pc.getDimensionScores().forEach((k, v) ->
                    System.out.println("    " + k + ": " + String.format("%.2f", v)));
        }
        System.out.println("========================");

        return scored.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PairRelation sendRequest(Long fromUserId, Long toUserId, String matchType, String message) {
        if (fromUserId.equals(toUserId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能向自己发送匹配请求");
        }

        // 检查是否已有关系
        PairRelation existing = findRelationBetween(fromUserId, toUserId, matchType);
        if (existing != null) {
            if ("accepted".equals(existing.getStatus())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "你们已经是伙伴了");
            }
            if ("pending".equals(existing.getStatus())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "已发送过匹配请求，请等待对方回应");
            }
            // 之前被拒绝/取消，可以重新发送
            existing.setStatus("pending");
            existing.setMessage(message);
            pairRelationMapper.updateById(existing);
            return existing;
        }

        PairRelation relation = PairRelation.builder()
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .status("pending")
                .matchType(matchType)
                .message(message)
                .build();
        pairRelationMapper.insert(relation);
        return relation;
    }

    @Override
    @Transactional
    public PairRelation acceptRequest(Long requestId) {
        PairRelation relation = pairRelationMapper.selectById(requestId);
        if (relation == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求不存在");
        }
        if (!"pending".equals(relation.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求已处理");
        }
        relation.setStatus("accepted");
        pairRelationMapper.updateById(relation);
        return relation;
    }

    @Override
    @Transactional
    public PairRelation rejectRequest(Long requestId) {
        PairRelation relation = pairRelationMapper.selectById(requestId);
        if (relation == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求不存在");
        }
        if (!"pending".equals(relation.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求已处理");
        }
        relation.setStatus("rejected");
        pairRelationMapper.updateById(relation);
        return relation;
    }

    @Override
    public List<MatchedPartner> getMyMatches(Long userId) {
        // 查找所有已接受的匹配关系
        List<PairRelation> relations = pairRelationMapper.selectList(
                new QueryWrapper<PairRelation>()
                        .eq("status", "accepted")
                        .and(w -> w.eq("from_user_id", userId).or().eq("to_user_id", userId)));

        return relations.stream()
                .map(r -> {
                    Long partnerId = r.getFromUserId().equals(userId) ? r.getToUserId() : r.getFromUserId();
                    return new MatchedPartner(r.getId(), partnerId, r.getMatchType(), r.getMessage());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<PairRelation> getPendingRequests(Long userId) {
        return pairRelationMapper.selectList(
                new QueryWrapper<PairRelation>()
                        .eq("to_user_id", userId)
                        .eq("status", "pending"));
    }

    @Override
    public List<PairRelation> getSentRequests(Long userId) {
        return pairRelationMapper.selectList(
                new QueryWrapper<PairRelation>()
                        .eq("from_user_id", userId)
                        .eq("status", "pending"));
    }

    @Override
    @Transactional
    public void cancelRequest(Long requestId, Long userId) {
        PairRelation relation = pairRelationMapper.selectById(requestId);
        if (relation == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求不存在");
        }
        if (!relation.getFromUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "只能取消自己发出的请求");
        }
        if (!"pending".equals(relation.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求已处理，无法取消");
        }
        relation.setStatus("cancelled");
        pairRelationMapper.updateById(relation);
    }

    @Override
    @Transactional
    public void unmatch(Long relationId, Long userId) {
        PairRelation relation = pairRelationMapper.selectById(relationId);
        if (relation == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "匹配关系不存在");
        }
        boolean isParticipant = relation.getFromUserId().equals(userId) || relation.getToUserId().equals(userId);
        if (!isParticipant) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无权操作此匹配");
        }
        if (!"accepted".equals(relation.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前状态无法解除匹配");
        }
        relation.setStatus("cancelled");
        pairRelationMapper.updateById(relation);
    }

    // ==================== 内部方法 ====================

    /**
     * 获取与当前用户已有关联的用户ID集合（排除这些用户不出现在推荐中）
     */
    private Set<Long> getRelatedUserIds(Long userId) {
        List<PairRelation> relations = pairRelationMapper.selectList(
                new QueryWrapper<PairRelation>()
                        .and(w -> w.eq("from_user_id", userId).or().eq("to_user_id", userId)));

        Set<Long> excluded = new HashSet<>();
        for (PairRelation r : relations) {
            if (r.getFromUserId().equals(userId)) {
                excluded.add(r.getToUserId());
            } else {
                excluded.add(r.getFromUserId());
            }
        }
        return excluded;
    }

    /**
     * 查找两个用户之间是否已存在匹配关系
     */
    private PairRelation findRelationBetween(Long userId1, Long userId2, String matchType) {
        // 检查 A->B 方向
        List<PairRelation> list = pairRelationMapper.selectList(
                new QueryWrapper<PairRelation>()
                        .eq("from_user_id", userId1)
                        .eq("to_user_id", userId2)
                        .eq("match_type", matchType));
        if (!list.isEmpty()) return list.get(0);

        // 检查 B->A 方向
        list = pairRelationMapper.selectList(
                new QueryWrapper<PairRelation>()
                        .eq("from_user_id", userId2)
                        .eq("to_user_id", userId1)
                        .eq("match_type", matchType));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 构建伙伴匹配的查询文本
     */
    private String buildPartnerQueryText(UserProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户画像信息：\n");
        if (profile.getMbtiType() != null) sb.append("MBTI类型：").append(profile.getMbtiType()).append("\n");
        if (profile.getTags() != null) sb.append("标签：").append(profile.getTags()).append("\n");
        if (profile.getInterests() != null) sb.append("兴趣爱好：").append(profile.getInterests()).append("\n");
        if (profile.getPreferences() != null) sb.append("偏好设置：").append(profile.getPreferences()).append("\n");
        if (profile.getPersonalityTraits() != null) sb.append("性格特征：").append(profile.getPersonalityTraits()).append("\n");
        if (profile.getCommunicationStyle() != null) sb.append("沟通风格：").append(profile.getCommunicationStyle()).append("\n");
        return sb.toString();
    }
}
