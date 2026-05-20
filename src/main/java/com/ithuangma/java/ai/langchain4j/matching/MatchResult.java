package com.ithuangma.java.ai.langchain4j.matching;

import java.util.List;

/**
 * 多维匹配结果
 * 包含总体评分和各维度评分详情
 */
public class MatchResult {

    private final double overallScore;
    private final List<DimensionScore> dimensionScores;

    public MatchResult(double overallScore, List<DimensionScore> dimensionScores) {
        this.overallScore = overallScore;
        this.dimensionScores = dimensionScores;
    }

    public double getOverallScore() {
        return overallScore;
    }

    public List<DimensionScore> getDimensionScores() {
        return dimensionScores;
    }

    /**
     * 单维度评分
     */
    public static class DimensionScore {
        private final String name;
        private final double score;
        private final double weight;

        public DimensionScore(String name, double score, double weight) {
            this.name = name;
            this.score = clamp(score);
            this.weight = weight;
        }

        public String getName() { return name; }
        public double getScore() { return score; }
        public double getWeight() { return weight; }
        public double getWeightedScore() { return score * weight; }

        private static double clamp(double val) {
            return Math.max(0.0, Math.min(1.0, val));
        }
    }
}
