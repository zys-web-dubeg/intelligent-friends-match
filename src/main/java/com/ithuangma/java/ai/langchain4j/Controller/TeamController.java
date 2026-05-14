package com.ithuangma.java.ai.langchain4j.Controller;

import com.ithuangma.java.ai.langchain4j.entity.TeamProfile;
import com.ithuangma.java.ai.langchain4j.Service.TeamService;
import com.ithuangma.java.ai.langchain4j.Bean.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    @Autowired
    private TeamService teamService;

    // 创建队伍
    @PostMapping
    public Result createTeam(@RequestBody TeamProfile teamProfile) {
        try {
            TeamProfile createdTeam = teamService.createTeam(teamProfile);
            return Result.ok(createdTeam);
        } catch (Exception e) {
            return Result.fail(500, "创建队伍失败: " + e.getMessage());
        }
    }

    // 加入队伍
    @PostMapping("/{teamId}/join")
    public Result joinTeam(@PathVariable Long teamId, @RequestParam("userId") Long userId) {
        try {
            boolean success = teamService.joinTeam(teamId, userId);
            if (success) {
                return Result.ok("成功加入队伍");
            } else {
                return Result.fail(400, "加入队伍失败");
            }
        } catch (Exception e) {
            return Result.fail(500, "加入队伍失败: " + e.getMessage());
        }
    }

    // 获取用户所属的队伍
    @GetMapping("/user/{userId}")
    public Result getUserTeams(@PathVariable Long userId) {
        try {
            List<TeamProfile> teams = teamService.getUserTeams(userId);
            return Result.ok(teams);
        } catch (Exception e) {
            return Result.fail(500, "获取队伍信息失败: " + e.getMessage());
        }
    }

    // 获取所有队伍
    @GetMapping("/all")
    public Result getAllTeams() {
        try {
            List<TeamProfile> teams = teamService.getAllTeams();
            return Result.ok(teams);
        } catch (Exception e) {
            return Result.fail(500, "获取队伍列表失败: " + e.getMessage());
        }
    }


    // 获取单个队伍详情
    @GetMapping("/{teamId}")
    public Result getTeamById(@PathVariable Long teamId) {
        try {
            TeamProfile team = teamService.getTeamById(teamId);
            if (team != null) {
                return Result.ok(team);
            } else {
                return Result.fail(404, "队伍不存在");
            }
        } catch (Exception e) {
            return Result.fail(500, "获取队伍信息失败: " + e.getMessage());
        }
    }

    // 获取匹配的队伍
    @GetMapping("/match/{userId}")
    public Result matchTeams(@PathVariable Long userId) {
        try {
            List<TeamProfile> matchedTeams = teamService.matchTeamsForUser(userId);
            return Result.ok(matchedTeams);
        } catch (Exception e) {
            return Result.fail(500, "匹配队伍失败: " + e.getMessage());
        }
    }

    // 获取队伍成员
    @GetMapping("/{teamId}/members")
    public Result getTeamMembers(@PathVariable Long teamId) {
        try {
            var members = teamService.getTeamMembers(teamId);
            return Result.ok(members);
        } catch (Exception e) {
            return Result.fail(500, "获取成员信息失败: " + e.getMessage());
        }
    }
}