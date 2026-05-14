package com.ithuangma.java.ai.langchain4j.Controller;

import com.ithuangma.java.ai.langchain4j.entity.TeamProfile;
import com.ithuangma.java.ai.langchain4j.entity.UserProfile;
import com.ithuangma.java.ai.langchain4j.Service.UserProfileService;
import com.ithuangma.java.ai.langchain4j.Service.TeamService;
import com.ithuangma.java.ai.langchain4j.Bean.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/user-profile")
public class UserProfileController {

    @Autowired
    private UserProfileService userProfileService;
    
    @Autowired
    private TeamService teamService;

    // 保存或更新用户画像
    @PostMapping
    public Result saveUserProfile(@RequestBody UserProfile userProfile) {
        try {
            UserProfile savedProfile = userProfileService.saveUserProfile(userProfile);
            return Result.ok(savedProfile);
        } catch (Exception e) {
            return Result.fail(500, "保存用户画像失败: " + e.getMessage());
        }
    }

    // 获取用户画像
    @GetMapping("/{userId}")
    public Result getUserProfile(@PathVariable Long userId) {
        try {
            UserProfile profile = userProfileService.getUserProfileByUserId(userId);
            if (profile != null) {
                return Result.ok(profile);
            } else {
                return Result.fail(404, "用户画像不存在");
            }
        } catch (Exception e) {
            return Result.fail(500, "获取用户画像失败: " + e.getMessage());
        }
    }

    // 批量获取用户画像
    @PostMapping("/batch")
    public Result getUserProfiles(@RequestBody List<Long> userIds) {
        try {
            List<UserProfile> profiles = userProfileService.getUserProfilesByUserIds(userIds);
            return Result.ok(profiles);
        } catch (Exception e) {
            return Result.fail(500, "获取用户画像失败: " + e.getMessage());
        }
    }

    // 更新用户标签
    @PutMapping("/{userId}/tags")
    public Result updateUserTags(@PathVariable Long userId, @RequestBody String tags) {
        try {
            UserProfile profile = userProfileService.updateUserTags(userId, tags);
            return Result.ok(profile);
        } catch (Exception e) {
            return Result.fail(500, "更新用户标签失败: " + e.getMessage());
        }
    }

    // 更新用户MBTI类型
    @PutMapping("/{userId}/mbti")
    public Result updateMbtiType(@PathVariable Long userId, @RequestParam String mbtiType) {
        try {
            UserProfile profile = userProfileService.updateMbtiType(userId, mbtiType);
            return Result.ok(profile);
        } catch (Exception e) {
            return Result.fail(500, "更新MBTI类型失败: " + e.getMessage());
        }
    }

    // 查找相似用户
    @GetMapping("/{userId}/similar-users")
    public Result findSimilarUsers(@PathVariable Long userId, 
                                   @RequestParam(defaultValue = "5") int limit) {
        try {
            List<UserProfile> similarUsers = userProfileService.findSimilarUsers(userId, limit);
            return Result.ok(similarUsers);
        } catch (Exception e) {
            return Result.fail(500, "查找相似用户失败: " + e.getMessage());
        }
    }

    // 根据画像查找适合的队伍
    @GetMapping("/{userId}/suitable-teams")
    public Result findSuitableTeams(@PathVariable Long userId) {
        try {
            List<Long> suitableTeamIds = userProfileService.findSuitableTeamsByProfile(userId);
            
            // 将ID列表转换为完整的队伍信息对象
            List<TeamProfile> suitableTeams = suitableTeamIds.stream()
                    .map(teamId -> teamService.getTeamById(teamId))
                    .filter(team -> team != null)
                    .collect(Collectors.toList());
            
            return Result.ok(suitableTeams);
        } catch (Exception e) {
            return Result.fail(500, "查找适合的队伍失败: " + e.getMessage());
        }
    }
}
