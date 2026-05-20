package com.ithuangma.java.ai.langchain4j.matching;

import com.ithuangma.java.ai.langchain4j.entity.TeamProfile;
import com.ithuangma.java.ai.langchain4j.entity.UserProfile;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 多维匹配评分引擎
 *
 * 核心思想：不同维度的匹配度分开计算，各自加权汇总，避免"一段文本模糊匹配"的问题。
 *
 * 团队匹配维度与权重：
 * - MBTI兼容性      25%：基于心理学规则的显式匹配
 * - 兴趣标签语义     40%：embedding 捕捉深层语义关联
 * - 性格特征         20%：性格互补或相似度
 * - 沟通风格         15%：沟通方式是否契合
 *
 * 相似用户维度与权重：
 * - MBTI兼容性      20%
 * - 兴趣标签语义     40%
 * - 兴趣爱好关键词   20%
 * - 沟通风格         20%
 */
@Component
public class MultiDimensionMatcher {

    // ==================== 团队匹配权重 ====================
    private static final double TEAM_MBTI_W = 0.25;
    private static final double TEAM_EMBEDDING_W = 0.40;
    private static final double TEAM_PERSONALITY_W = 0.20;
    private static final double TEAM_COMMUNICATION_W = 0.15;

    // ==================== 用户相似度权重 ====================
    private static final double SIM_MBTI_W = 0.20;
    private static final double SIM_EMBEDDING_W = 0.40;
    private static final double SIM_INTEREST_W = 0.20;
    private static final double SIM_COMMUNICATION_W = 0.20;

    private final EmbeddingModel embeddingModel;

    public MultiDimensionMatcher(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    // ==================== 对外API ====================

    /**
     * 计算用户与队伍的多维匹配分数
     */
    public MatchResult scoreUserTeamMatch(UserProfile user, TeamProfile team) {
        List<MatchResult.DimensionScore> dims = new ArrayList<>();

        // 1. MBTI 兼容性（从队伍约束文本中提取 MBTI 偏好）
        String preferredMbti = MbtiCompatibility.extractFromText(team.getPersonalityConstraint());
        double mbtiScore = preferredMbti != null
                ? MbtiCompatibility.forTeamMatching(user.getMbtiType(), preferredMbti)
                : 0.5;
        dims.add(new MatchResult.DimensionScore("MBTI匹配", mbtiScore, TEAM_MBTI_W));

        // 2. 兴趣标签语义相似度
        double embeddingScore = calcEmbeddingSimilarity(
                buildUserEmbeddingText(user),
                team.getPersonalityConstraint());
        dims.add(new MatchResult.DimensionScore("兴趣标签", embeddingScore, TEAM_EMBEDDING_W));

        // 3. 性格特征匹配
        double personalityScore = calcFieldSimilarity(
                user.getPersonalityTraits(), team.getPersonalityConstraint());
        dims.add(new MatchResult.DimensionScore("性格特征", personalityScore, TEAM_PERSONALITY_W));

        // 4. 沟通风格匹配
        double commScore = calcFieldSimilarity(
                user.getCommunicationStyle(), team.getPersonalityConstraint());
        dims.add(new MatchResult.DimensionScore("沟通风格", commScore, TEAM_COMMUNICATION_W));

        return buildResult(dims);
    }

    /**
     * 计算两个用户的多维相似度
     */
    public MatchResult scoreUserSimilarity(UserProfile u1, UserProfile u2) {
        List<MatchResult.DimensionScore> dims = new ArrayList<>();

        double mbtiScore = MbtiCompatibility.forSimilarityMatching(u1.getMbtiType(), u2.getMbtiType());
        dims.add(new MatchResult.DimensionScore("MBTI", mbtiScore, SIM_MBTI_W));

        double embeddingScore = calcEmbeddingSimilarity(
                buildUserEmbeddingText(u1),
                buildUserEmbeddingText(u2));
        dims.add(new MatchResult.DimensionScore("兴趣标签", embeddingScore, SIM_EMBEDDING_W));

        double interestScore = calcFieldSimilarity(u1.getInterests(), u2.getInterests());
        dims.add(new MatchResult.DimensionScore("兴趣爱好", interestScore, SIM_INTEREST_W));

        double commScore = calcFieldSimilarity(u1.getCommunicationStyle(), u2.getCommunicationStyle());
        dims.add(new MatchResult.DimensionScore("沟通风格", commScore, SIM_COMMUNICATION_W));

        return buildResult(dims);
    }

    // ==================== 伙伴匹配权重（按类型） ====================
    // 恋爱匹配：互补性更重要
    private static final double DATE_MBTI_W = 0.25;
    private static final double DATE_EMBEDDING_W = 0.25;
    private static final double DATE_INTEREST_W = 0.25;
    private static final double DATE_COMMUNICATION_W = 0.25;

    // 学习/兴趣/运动/游戏/交友：相似性更重要
    private static final double FRIEND_MBTI_W = 0.15;
    private static final double FRIEND_EMBEDDING_W = 0.35;
    private static final double FRIEND_INTEREST_W = 0.30;
    private static final double FRIEND_COMMUNICATION_W = 0.20;

    // ==================== 伙伴匹配 ====================

    /**
     * 计算两个用户之间的伙伴匹配度
     * 根据匹配类型使用不同的评分策略和权重
     *
     * @param matchType study(学习)/hobby(兴趣)/sports(运动)/game(游戏)/friend(交友)/date(恋爱)
     */
    public MatchResult scorePartnerMatch(UserProfile u1, UserProfile u2, String matchType) {
        boolean isRomantic = "date".equals(matchType);

        double mbtiW = isRomantic ? DATE_MBTI_W : FRIEND_MBTI_W;
        double embeddingW = isRomantic ? DATE_EMBEDDING_W : FRIEND_EMBEDDING_W;
        double interestW = isRomantic ? DATE_INTEREST_W : FRIEND_INTEREST_W;
        double commW = isRomantic ? DATE_COMMUNICATION_W : FRIEND_COMMUNICATION_W;

        List<MatchResult.DimensionScore> dims = new ArrayList<>();

        // MBTI：恋爱用互补评分，其他用相似评分
        double mbtiScore = isRomantic
                ? MbtiCompatibility.forTeamMatching(u1.getMbtiType(), u2.getMbtiType())
                : MbtiCompatibility.forSimilarityMatching(u1.getMbtiType(), u2.getMbtiType());
        dims.add(new MatchResult.DimensionScore("MBTI", mbtiScore, mbtiW));

        // 兴趣标签语义相似度
        double embeddingScore = calcEmbeddingSimilarity(
                buildUserEmbeddingText(u1), buildUserEmbeddingText(u2));
        dims.add(new MatchResult.DimensionScore("兴趣标签", embeddingScore, embeddingW));

        // 兴趣爱好关键词匹配
        double interestScore = calcFieldSimilarity(u1.getInterests(), u2.getInterests());
        dims.add(new MatchResult.DimensionScore("爱好契合", interestScore, interestW));

        // 沟通风格匹配
        double commScore = calcFieldSimilarity(u1.getCommunicationStyle(), u2.getCommunicationStyle());
        dims.add(new MatchResult.DimensionScore("沟通风格", commScore, commW));

        return buildResult(dims);
    }

    // ==================== 内部方法 ====================

    private MatchResult buildResult(List<MatchResult.DimensionScore> dims) {
        double overall = dims.stream()
                .mapToDouble(MatchResult.DimensionScore::getWeightedScore)
                .sum();
        return new MatchResult(overall, dims);
    }

    private double calcEmbeddingSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isBlank() || text2.isBlank()) {
            return 0.5;
        }
        try {
            Embedding e1 = embeddingModel.embed(text1).content();
            Embedding e2 = embeddingModel.embed(text2).content();
            return cosineSimilarity(e1.vectorAsList(), e2.vectorAsList());
        } catch (Exception e) {
            return 0.5;
        }
    }

    private double calcFieldSimilarity(String field1, String field2) {
        if (field1 == null || field2 == null || field1.isBlank() || field2.isBlank()) {
            return 0.5; // 缺少数据时给中性分
        }
        return calcEmbeddingSimilarity(field1, field2);
    }

    private double cosineSimilarity(List<Float> v1, List<Float> v2) {
        if (v1.size() != v2.size()) return 0;
        double dot = 0, n1 = 0, n2 = 0;
        for (int i = 0; i < v1.size(); i++) {
            dot += (double) v1.get(i) * v2.get(i);
            n1 += (double) v1.get(i) * v1.get(i);
            n2 += (double) v2.get(i) * v2.get(i);
        }
        if (n1 == 0 || n2 == 0) return 0;
        return dot / (Math.sqrt(n1) * Math.sqrt(n2));
    }

    /**
     * 构建用于 embedding 的用户描述文本（标签 + 兴趣）
     */
    private String buildUserEmbeddingText(UserProfile user) {
        StringBuilder sb = new StringBuilder();
        if (user.getTags() != null && !user.getTags().isBlank()) {
            sb.append("标签：").append(user.getTags()).append("\n");
        }
        if (user.getInterests() != null && !user.getInterests().isBlank()) {
            sb.append("兴趣爱好：").append(user.getInterests()).append("\n");
        }
        if (user.getPreferences() != null && !user.getPreferences().isBlank()) {
            sb.append("偏好：").append(user.getPreferences()).append("\n");
        }
        String result = sb.toString();
        return result.isBlank() ? "默认用户画像" : result;
    }
}
