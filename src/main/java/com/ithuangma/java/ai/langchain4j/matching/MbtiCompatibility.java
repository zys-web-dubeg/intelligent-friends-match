package com.ithuangma.java.ai.langchain4j.matching;

/**
 * MBTI (Myers-Briggs Type Indicator) 兼容性评分工具
 *
 * 四维人格维度：E(外向)/I(内向), S(实感)/N(直觉), T(思考)/F(情感), J(判断)/P(感知)
 *
 * 团队匹配场景：互补型性格在团队中能带来更好的协作效果
 * 相似用户场景：相同维度越多，性格越相似，沟通成本越低
 */
public class MbtiCompatibility {

    /**
     * 团队匹配场景下的MBTI兼容性评分 (0.0 ~ 1.0)
     *
     * E/I: 外向+内向互补 → 平衡团队活力与深思 (互补: 0.85, 相同: 0.50)
     * S/N: 相同更好沟通 → 避免认知方式冲突 (相同: 0.80, 互补: 0.45)
     * T/F: 互补平衡逻辑与情感决策 (互补: 0.75, 相同: 0.50)
     * J/P: 互补平衡计划与灵活应变 (互补: 0.75, 相同: 0.50)
     */
    public static double forTeamMatching(String mbti1, String mbti2) {
        if (isInvalid(mbti1) || isInvalid(mbti2)) return 0.5;

        double ei = (mbti1.charAt(0) != mbti2.charAt(0)) ? 0.85 : 0.50;
        double sn = (mbti1.charAt(1) == mbti2.charAt(1)) ? 0.80 : 0.45;
        double tf = (mbti1.charAt(2) != mbti2.charAt(2)) ? 0.75 : 0.50;
        double jp = (mbti1.charAt(3) != mbti2.charAt(3)) ? 0.75 : 0.50;

        return (ei + sn + tf + jp) / 4.0;
    }

    /**
     * 相似用户匹配场景下的MBTI兼容性评分 (0.0 ~ 1.0)
     * 相同维度比例越高越相似
     */
    public static double forSimilarityMatching(String mbti1, String mbti2) {
        if (isInvalid(mbti1) || isInvalid(mbti2)) return 0.5;

        int matches = 0;
        for (int i = 0; i < 4; i++) {
            if (mbti1.charAt(i) == mbti2.charAt(i)) matches++;
        }
        return matches / 4.0;
    }

    /**
     * 从文本中提取 MBTI 类型（如 "INTJ", "ENFP"）
     * 用于从队伍约束条件中解析 MBTI 偏好
     */
    public static String extractFromText(String text) {
        if (text == null || text.isBlank()) return null;
        var pattern = java.util.regex.Pattern.compile("\\b[EI][NS][TF][JP]\\b");
        var matcher = pattern.matcher(text.toUpperCase());
        return matcher.find() ? matcher.group() : null;
    }

    private static boolean isInvalid(String mbti) {
        return mbti == null || mbti.length() < 4;
    }
}
