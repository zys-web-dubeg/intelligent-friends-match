package com.ithuangma.java.ai.langchain4j.Service;

import com.ithuangma.java.ai.langchain4j.entity.UserProfile;
import java.util.List;
import java.util.Map;

public interface UserProfileService {
    // 创建或更新用户画像
    UserProfile saveUserProfile(UserProfile userProfile);
    
    // 根据用户ID获取用户画像
    UserProfile getUserProfileByUserId(Long userId);
    
    // 根据用户ID列表批量获取用户画像
    List<UserProfile> getUserProfilesByUserIds(List<Long> userIds);
    
    // 更新用户标签
    UserProfile updateUserTags(Long userId, String tags);
    
    // 更新用户MBTI类型
    UserProfile updateMbtiType(Long userId, String mbtiType);
    
    // 根据画像特征匹配相似用户
    List<UserProfile> findSimilarUsers(Long userId, int limit);
    
    // 根据画像特征匹配适合的队伍
    List<Long> findSuitableTeamsByProfile(Long userId);
}