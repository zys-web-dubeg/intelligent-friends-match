package com.ithuangma.java.ai.langchain4j.Tools;

import com.ithuangma.java.ai.langchain4j.Service.PartnerMatchingService;
import com.ithuangma.java.ai.langchain4j.Service.TeamService;
import com.ithuangma.java.ai.langchain4j.Service.UserProfileService;
import com.ithuangma.java.ai.langchain4j.entity.PairRelation;
import com.ithuangma.java.ai.langchain4j.entity.TeamProfile;
import com.ithuangma.java.ai.langchain4j.entity.UserProfile;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 智能伙伴匹配工具类
 * 将伙伴匹配相关的业务Service封装为LangChain4j的@Tool方法，
 * 让AI助手可以根据用户意图自动选择调用合适的工具
 */
@Component
public class PartnerMatchingTools {

    @Autowired
    private PartnerMatchingService partnerMatchingService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private TeamService teamService;

    // ==================== 伙伴推荐相关工具 ====================

    /**
     * 查找推荐伙伴
     * 根据用户画像和匹配类型，推荐最合适的伙伴候选人
     */
    @Tool(name = "查找推荐伙伴", value = "根据用户的画像信息，查找并推荐最合适的伙伴候选人。" +
            "matchType参数可选值：friend(找朋友)、study(找学习伙伴)、work(找工作伙伴)、love(找恋爱对象)。" +
            "返回推荐列表，包含每位候选人的画像信息和匹配评分。")
    public String recommendPartners(
            @P(value = "当前用户的ID") Long userId,
            @P(value = "匹配类型，可选值：friend、study、work、love") String matchType,
            @P(value = "返回推荐结果的最大数量，默认5") int limit) {
        try {
            List<PartnerMatchingService.PartnerCandidate> candidates =
                    partnerMatchingService.findPotentialPartners(userId, matchType, limit);

            if (candidates.isEmpty()) {
                return "暂时没有找到合适的伙伴推荐，可能是因为你的画像信息还不够完善，" +
                        "建议先完善你的个人标签、兴趣爱好、性格特征等信息。";
            }

            // 将推荐结果格式化为易读的文本，方便AI助手理解和转述给用户
            StringBuilder sb = new StringBuilder();
            sb.append("为你找到了以下").append(matchType).append("类型的伙伴推荐：\n\n");

            for (int i = 0; i < candidates.size(); i++) {
                PartnerMatchingService.PartnerCandidate candidate = candidates.get(i);
                UserProfile profile = candidate.getUserProfile();

                sb.append("第").append(i + 1).append("名：\n");
                sb.append("  用户ID：").append(profile.getUserId()).append("\n");
                if (profile.getMbtiType() != null) {
                    sb.append("  MBTI类型：").append(profile.getMbtiType()).append("\n");
                }
                if (profile.getInterests() != null) {
                    sb.append("  兴趣爱好：").append(profile.getInterests()).append("\n");
                }
                if (profile.getTags() != null) {
                    sb.append("  标签：").append(profile.getTags()).append("\n");
                }
                if (profile.getPersonalityTraits() != null) {
                    sb.append("  性格特征：").append(profile.getPersonalityTraits()).append("\n");
                }
                sb.append("  综合匹配度：").append(String.format("%.0f%%", candidate.getMatchScore() * 100)).append("\n");

                // 展示各维度的匹配评分
                if (candidate.getDimensionScores() != null && !candidate.getDimensionScores().isEmpty()) {
                    sb.append("  各维度评分：");
                    candidate.getDimensionScores().forEach((dim, score) ->
                            sb.append(dim).append("=").append(String.format("%.0f%%", score * 100)).append(" "));
                    sb.append("\n");
                }
                sb.append("\n");
            }

            sb.append("你可以告诉我想和哪位候选者建立匹配关系，我来帮你发送匹配请求。");
            return sb.toString();
        } catch (Exception e) {
            return "查找推荐伙伴时出现异常：" + e.getMessage();
        }
    }

    /**
     * 查找相似用户
     */
    @Tool(name = "查找相似用户", value = "根据用户的画像信息，查找与该用户兴趣、性格等方面最相似的其他用户。" +
            "返回相似用户列表，包含他们的画像信息。")
    public String findSimilarUsers(
            @P(value = "当前用户的ID") Long userId,
            @P(value = "返回结果的最大数量，默认5") int limit) {
        try {
            List<UserProfile> similarUsers = userProfileService.findSimilarUsers(userId, limit);

            if (similarUsers.isEmpty()) {
                return "暂时没有找到与你相似的用户，建议先完善你的个人画像信息。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("为你找到了以下与你相似的用户：\n\n");

            for (int i = 0; i < similarUsers.size(); i++) {
                UserProfile profile = similarUsers.get(i);
                sb.append("第").append(i + 1).append("名：\n");
                sb.append("  用户ID：").append(profile.getUserId()).append("\n");
                if (profile.getMbtiType() != null) {
                    sb.append("  MBTI类型：").append(profile.getMbtiType()).append("\n");
                }
                if (profile.getInterests() != null) {
                    sb.append("  兴趣爱好：").append(profile.getInterests()).append("\n");
                }
                if (profile.getTags() != null) {
                    sb.append("  标签：").append(profile.getTags()).append("\n");
                }
                if (profile.getPersonalityTraits() != null) {
                    sb.append("  性格特征：").append(profile.getPersonalityTraits()).append("\n");
                }
                sb.append("\n");
            }

            sb.append("如果你对某位用户感兴趣，可以告诉我想和他们建立匹配关系。");
            return sb.toString();
        } catch (Exception e) {
            return "查找相似用户时出现异常：" + e.getMessage();
        }
    }

    // ==================== 匹配请求相关工具 ====================

    /**
     * 发送匹配请求
     */
    @Tool(name = "发送匹配请求", value = "向指定用户发送匹配请求，建立伙伴关系。" +
            "需要提供目标用户的ID、匹配类型和留言消息。" +
            "matchType可选值：friend、study、work、love。" +
            "发送前请先确认用户确实想与目标用户建立匹配。")
    public String sendMatchRequest(
            @P(value = "发送请求的用户ID（当前用户）") Long fromUserId,
            @P(value = "目标用户的ID") Long toUserId,
            @P(value = "匹配类型，可选值：friend、study、work、love") String matchType,
            @P(value = "附带的留言消息，用于向对方介绍自己") String message) {
        try {
            PairRelation relation = partnerMatchingService.sendRequest(fromUserId, toUserId, matchType, message);
            return "匹配请求已成功发送！请求ID：" + relation.getId() +
                    "\n匹配类型：" + matchType +
                    "\n留言：" + message +
                    "\n对方尚未回应，请耐心等待。你也可以随时查看你的匹配请求状态。";
        } catch (Exception e) {
            return "发送匹配请求失败：" + e.getMessage();
        }
    }

    /**
     * 查看已匹配的伙伴列表
     */
    @Tool(name = "查看已匹配伙伴", value = "查看当前用户已经成功匹配的所有伙伴列表。" +
            "返回每位伙伴的ID、匹配类型和留言信息。")
    public String getMyMatches(@P(value = "当前用户的ID") Long userId) {
        try {
            List<PartnerMatchingService.MatchedPartner> matches = partnerMatchingService.getMyMatches(userId);

            if (matches.isEmpty()) {
                return "你目前还没有已匹配的伙伴。你可以让我帮你推荐一些合适的伙伴，然后发送匹配请求。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("你目前已匹配的伙伴共有").append(matches.size()).append("位：\n\n");

            for (PartnerMatchingService.MatchedPartner match : matches) {
                sb.append("  关系ID：").append(match.getRelationId()).append("\n");
                sb.append("  伙伴用户ID：").append(match.getPartnerUserId()).append("\n");
                sb.append("  匹配类型：").append(match.getMatchType()).append("\n");
                if (match.getMessage() != null) {
                    sb.append("  留言：").append(match.getMessage()).append("\n");
                }
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "查看已匹配伙伴时出现异常：" + e.getMessage();
        }
    }

    /**
     * 查看待处理的匹配请求
     */
    @Tool(name = "查看待处理请求", value = "查看别人发给当前用户的、尚未处理的匹配请求。" +
            "用户可以选择接受或拒绝这些请求。")
    public String getPendingRequests(@P(value = "当前用户的ID") Long userId) {
        try {
            List<PairRelation> requests = partnerMatchingService.getPendingRequests(userId);

            if (requests.isEmpty()) {
                return "你目前没有待处理的匹配请求。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("你有").append(requests.size()).append("条待处理的匹配请求：\n\n");

            for (PairRelation request : requests) {
                sb.append("  请求ID：").append(request.getId()).append("\n");
                sb.append("  发送者用户ID：").append(request.getFromUserId()).append("\n");
                sb.append("  匹配类型：").append(request.getMatchType()).append("\n");
                if (request.getMessage() != null) {
                    sb.append("  留言：").append(request.getMessage()).append("\n");
                }
                sb.append("\n");
            }

            sb.append("如果你想接受或拒绝某条请求，请告诉我请求ID和你的决定。");
            return sb.toString();
        } catch (Exception e) {
            return "查看待处理请求时出现异常：" + e.getMessage();
        }
    }

    /**
     * 接受匹配请求
     */
    @Tool(name = "接受匹配请求", value = "接受别人发来的匹配请求，建立伙伴关系。" +
            "接受后双方即成为匹配伙伴。")
    public String acceptMatchRequest(@P(value = "要接受的匹配请求ID") Long requestId) {
        try {
            PairRelation relation = partnerMatchingService.acceptRequest(requestId);
            return "已成功接受匹配请求！你现在和用户" + relation.getFromUserId() +
                    "已成为" + relation.getMatchType() + "类型的伙伴。";
        } catch (Exception e) {
            return "接受匹配请求失败：" + e.getMessage();
        }
    }

    /**
     * 拒绝匹配请求
     */
    @Tool(name = "拒绝匹配请求", value = "拒绝别人发来的匹配请求。拒绝后对方不会再收到此请求的通知。")
    public String rejectMatchRequest(@P(value = "要拒绝的匹配请求ID") Long requestId) {
        try {
            PairRelation relation = partnerMatchingService.rejectRequest(requestId);
            return "已拒绝来自用户" + relation.getFromUserId() + "的匹配请求。";
        } catch (Exception e) {
            return "拒绝匹配请求失败：" + e.getMessage();
        }
    }

    // ==================== 用户画像相关工具 ====================

    /**
     * 获取用户画像信息
     */
    @Tool(name = "获取用户画像", value = "获取指定用户的个人画像信息，包括MBTI类型、兴趣爱好、标签、性格特征、沟通风格等。" +
            "用于了解用户的个人信息，以便做出更精准的推荐。")
    public String getUserProfile(@P(value = "要查看的用户ID") Long userId) {
        try {
            UserProfile profile = userProfileService.getUserProfileByUserId(userId);

            if (profile == null) {
                return "该用户尚未创建个人画像，建议先完善个人信息以获得更精准的推荐。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("用户画像信息（用户ID：").append(userId).append("）：\n");
            if (profile.getMbtiType() != null) {
                sb.append("MBTI类型：").append(profile.getMbtiType()).append("\n");
            }
            if (profile.getTags() != null) {
                sb.append("标签：").append(profile.getTags()).append("\n");
            }
            if (profile.getInterests() != null) {
                sb.append("兴趣爱好：").append(profile.getInterests()).append("\n");
            }
            if (profile.getPreferences() != null) {
                sb.append("偏好设置：").append(profile.getPreferences()).append("\n");
            }
            if (profile.getPersonalityTraits() != null) {
                sb.append("性格特征：").append(profile.getPersonalityTraits()).append("\n");
            }
            if (profile.getCommunicationStyle() != null) {
                sb.append("沟通风格：").append(profile.getCommunicationStyle()).append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "获取用户画像时出现异常：" + e.getMessage();
        }
    }

    /**
     * 更新用户标签
     */
    @Tool(name = "更新用户标签", value = "更新用户的个人标签，标签可以帮助系统更精准地推荐伙伴和队伍。" +
            "标签格式建议用逗号分隔，例如：编程,AI,阅读,系统设计")
    public String updateUserTags(
            @P(value = "用户ID") Long userId,
            @P(value = "新的标签内容，建议用逗号分隔") String tags) {
        try {
            UserProfile profile = userProfileService.updateUserTags(userId, tags);
            return "用户标签已更新为：" + tags + "\n系统会根据新标签重新为你推荐更合适的伙伴和队伍。";
        } catch (Exception e) {
            return "更新用户标签时出现异常：" + e.getMessage();
        }
    }

    // ==================== 队伍相关工具 ====================

    /**
     * 推荐适合的队伍
     */
    @Tool(name = "推荐适合队伍", value = "根据用户的画像信息，推荐最适合该用户加入的队伍。" +
            "返回推荐队伍列表，包含队伍名称和匹配评分。")
    public String recommendTeams(@P(value = "当前用户的ID") Long userId) {
        try {
            List<TeamProfile> matchedTeams = teamService.matchTeamsForUser(userId);

            if (matchedTeams.isEmpty()) {
                return "暂时没有找到适合你的队伍推荐，可能是因为你的画像信息还不够完善，" +
                        "或者目前还没有与你匹配度较高的队伍。建议先完善个人画像，或者创建一个新队伍。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("根据你的画像信息，为你推荐以下队伍：\n\n");

            for (int i = 0; i < matchedTeams.size(); i++) {
                TeamProfile team = matchedTeams.get(i);
                sb.append("第").append(i + 1).append("名：\n");
                sb.append("  队伍ID：").append(team.getId()).append("\n");
                sb.append("  队伍名称：").append(team.getName()).append("\n");
                if (team.getPersonalityConstraint() != null) {
                    sb.append("  队伍特征：").append(team.getPersonalityConstraint()).append("\n");
                }
                sb.append("  访问级别：");
                switch (team.getAccessLevel()) {
                    case 0: sb.append("公开（可直接加入）\n"); break;
                    case 1: sb.append("审核（需匹配评估后加入）\n"); break;
                    case 2: sb.append("私有（需邀请才能加入）\n"); break;
                    default: sb.append("未知\n"); break;
                }
                sb.append("\n");
            }

            sb.append("如果你想加入某个队伍，请告诉我队伍ID，我来帮你操作。");
            return sb.toString();
        } catch (Exception e) {
            return "推荐队伍时出现异常：" + e.getMessage();
        }
    }

    /**
     * 加入队伍
     */
    @Tool(name = "加入队伍", value = "让用户加入指定的队伍。" +
            "公开队伍可以直接加入，审核队伍需要通过匹配评估，私有队伍需要邀请。" +
            "加入前请确认用户确实想加入该队伍。")
    public String joinTeam(
            @P(value = "要加入的队伍ID") Long teamId,
            @P(value = "当前用户的ID") Long userId) {
        try {
            boolean success = teamService.joinTeam(teamId, userId);
            if (success) {
                TeamProfile team = teamService.getTeamById(teamId);
                return "已成功加入队伍「" + (team != null ? team.getName() : teamId) + "」！\n" +
                        "你现在可以和队伍成员一起交流合作了。";
            } else {
                return "加入队伍失败，可能是队伍不存在或你不满足加入条件。";
            }
        } catch (Exception e) {
            return "加入队伍失败：" + e.getMessage();
        }
    }

    /**
     * 查看我的队伍
     */
    @Tool(name = "查看我的队伍", value = "查看当前用户已加入的所有队伍列表。")
    public String getMyTeams(@P(value = "当前用户的ID") Long userId) {
        try {
            List<TeamProfile> teams = teamService.getUserTeams(userId);

            if (teams.isEmpty()) {
                return "你目前还没有加入任何队伍。我可以帮你推荐一些适合你的队伍，或者你也可以创建一个新队伍。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("你目前已加入").append(teams.size()).append("个队伍：\n\n");

            for (TeamProfile team : teams) {
                sb.append("  队伍ID：").append(team.getId()).append("\n");
                sb.append("  队伍名称：").append(team.getName()).append("\n");
                if (team.getPersonalityConstraint() != null) {
                    sb.append("  队伍特征：").append(team.getPersonalityConstraint()).append("\n");
                }
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "查看队伍列表时出现异常：" + e.getMessage();
        }
    }

    /**
     * 查看队伍详情
     */
    @Tool(name = "查看队伍详情", value = "根据队伍ID查看队伍的详细信息，包括名称、特征描述、访问级别等。")
    public String getTeamDetail(@P(value = "队伍ID") Long teamId) {
        try {
            TeamProfile team = teamService.getTeamById(teamId);
            if (team == null) {
                return "未找到该队伍，请核对队伍ID。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("队伍详情：\n");
            sb.append("  队伍ID：").append(team.getId()).append("\n");
            sb.append("  队伍名称：").append(team.getName()).append("\n");
            if (team.getPersonalityConstraint() != null) {
                sb.append("  队伍特征：").append(team.getPersonalityConstraint()).append("\n");
            }
            sb.append("  访问级别：");
            switch (team.getAccessLevel()) {
                case 0: sb.append("公开\n"); break;
                case 1: sb.append("审核\n"); break;
                case 2: sb.append("私有\n"); break;
                default: sb.append("未知\n"); break;
            }

            // 获取队伍成员信息
            var members = teamService.getTeamMembers(teamId);
            sb.append("  当前成员数：").append(members.size()).append("人\n");

            return sb.toString();
        } catch (Exception e) {
            return "查看队伍详情时出现异常：" + e.getMessage();
        }
    }
}