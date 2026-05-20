package com.ithuangma.java.ai.langchain4j.Service;

import com.ithuangma.java.ai.langchain4j.entity.PairRelation;
import com.ithuangma.java.ai.langchain4j.entity.UserProfile;

import java.util.List;
import java.util.Map;

public interface PartnerMatchingService {

    /**
     * 查找潜在匹配伙伴
     */
    List<PartnerCandidate> findPotentialPartners(Long userId, String matchType, int limit);

    /**
     * 发送匹配请求
     */
    PairRelation sendRequest(Long fromUserId, Long toUserId, String matchType, String message);

    /**
     * 接受匹配请求
     */
    PairRelation acceptRequest(Long requestId);

    /**
     * 拒绝匹配请求
     */
    PairRelation rejectRequest(Long requestId);

    /**
     * 获取已匹配的伙伴列表
     */
    List<MatchedPartner> getMyMatches(Long userId);

    /**
     * 获取收到的待处理请求
     */
    List<PairRelation> getPendingRequests(Long userId);

    /**
     * 获取发出的待处理请求
     */
    List<PairRelation> getSentRequests(Long userId);

    /**
     * 取消发出的请求
     */
    void cancelRequest(Long requestId, Long userId);

    /**
     * 解除匹配
     */
    void unmatch(Long relationId, Long userId);

    /**
     * 伙伴推荐候选
     */
    class PartnerCandidate {
        private UserProfile userProfile;
        private double matchScore;
        private Map<String, Double> dimensionScores;

        public PartnerCandidate() {}

        public PartnerCandidate(UserProfile userProfile, double matchScore, Map<String, Double> dimensionScores) {
            this.userProfile = userProfile;
            this.matchScore = matchScore;
            this.dimensionScores = dimensionScores;
        }

        public UserProfile getUserProfile() { return userProfile; }
        public void setUserProfile(UserProfile userProfile) { this.userProfile = userProfile; }
        public double getMatchScore() { return matchScore; }
        public void setMatchScore(double matchScore) { this.matchScore = matchScore; }
        public Map<String, Double> getDimensionScores() { return dimensionScores; }
        public void setDimensionScores(Map<String, Double> dimensionScores) { this.dimensionScores = dimensionScores; }
    }

    /**
     * 已匹配的伙伴信息
     */
    class MatchedPartner {
        private Long relationId;
        private Long partnerUserId;
        private String matchType;
        private String message;

        public MatchedPartner() {}

        public MatchedPartner(Long relationId, Long partnerUserId, String matchType, String message) {
            this.relationId = relationId;
            this.partnerUserId = partnerUserId;
            this.matchType = matchType;
            this.message = message;
        }

        public Long getRelationId() { return relationId; }
        public void setRelationId(Long relationId) { this.relationId = relationId; }
        public Long getPartnerUserId() { return partnerUserId; }
        public void setPartnerUserId(Long partnerUserId) { this.partnerUserId = partnerUserId; }
        public String getMatchType() { return matchType; }
        public void setMatchType(String matchType) { this.matchType = matchType; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
