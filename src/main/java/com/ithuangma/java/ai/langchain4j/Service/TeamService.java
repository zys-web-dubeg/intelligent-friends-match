package com.ithuangma.java.ai.langchain4j.Service;

import com.ithuangma.java.ai.langchain4j.entity.TeamProfile;
import com.ithuangma.java.ai.langchain4j.entity.TeamMemberRelation;
import java.util.List;

public interface TeamService {
    // 创建队伍
    TeamProfile createTeam(TeamProfile teamProfile);
    
    // 加入队伍
    boolean joinTeam(Long teamId, Long userId);

    // 获取所有队伍
    List<TeamProfile> getAllTeams();

    // 获取单个队伍详情
    TeamProfile getTeamById(Long teamId);

    // 获取用户所属的队伍
    List<TeamProfile> getUserTeams(Long userId);
    
    // 队伍匹配算法 - 根据用户画像和队伍特征匹配
    List<TeamProfile> matchTeamsForUser(Long userId);
    
    // 获取队伍成员
    List<TeamMemberRelation> getTeamMembers(Long teamId);
}