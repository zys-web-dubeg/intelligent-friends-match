package com.ithuangma.java.ai.langchain4j.Controller;

import com.ithuangma.java.ai.langchain4j.Bean.ChatForm;
import com.ithuangma.java.ai.langchain4j.Bean.Result;
import com.ithuangma.java.ai.langchain4j.Service.PartnerMatchingService;
import com.ithuangma.java.ai.langchain4j.Service.StatisticsService;
import com.ithuangma.java.ai.langchain4j.assistant.PartnerMatchingAgent;
import com.ithuangma.java.ai.langchain4j.entity.PairRelation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@Tag(name = "智能伙伴匹配")
@RequestMapping("/api/v1/partners")
public class PartnerMatchingController {

    @Autowired
    private PartnerMatchingService partnerMatchingService;

    @Autowired
    private PartnerMatchingAgent partnerMatchingAgent;

    @Autowired
    private StatisticsService statisticsService;

    // ==================== AI对话接口 ====================

    /**
     * 智能匹配助手对话接口
     * 用户通过对话让AI助手帮助找伙伴、找队伍、管理匹配请求
     * 在用户消息中隐式注入userId，让AI知道当前用户身份
     */
    @Operation(summary = "与智能匹配助手对话")
    @PostMapping(value = "/chat", produces = "text/stream;charset=utf-8")
    public Flux<String> chatWithAssistant(@RequestBody ChatForm chatForm) {
        // 记录API请求统计
        statisticsService.recordApiRequest("partner_matching_agent");

        // 将userId信息注入到用户消息中，让AI知道当前用户身份
        // 这样AI调用Tool时就知道该用哪个userId
        String enrichedMessage;
        if (chatForm.getUserId() != null) {
            enrichedMessage = "[当前登录用户ID: " + chatForm.getUserId() + "] " + chatForm.getMessage();
        } else {
            enrichedMessage = chatForm.getMessage();
        }

        return partnerMatchingAgent.chat(chatForm.getMemoryId(), enrichedMessage);
    }

    // ==================== REST接口（保留原有的直接调用方式） ====================

    /**
     * 获取伙伴推荐列表
     */
    @GetMapping("/recommend/{userId}")
    public Result recommendPartners(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "friend") String matchType,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            List<PartnerMatchingService.PartnerCandidate> candidates =
                    partnerMatchingService.findPotentialPartners(userId, matchType, limit);
            return Result.ok(candidates);
        } catch (Exception e) {
            return Result.fail(500, "获取伙伴推荐失败: " + e.getMessage());
        }
    }

    /**
     * 发送匹配请求
     */
    @PostMapping("/request")
    public Result sendRequest(@RequestBody SendRequestParam param) {
        try {
            PairRelation relation = partnerMatchingService.sendRequest(
                    param.getFromUserId(), param.getToUserId(), param.getMatchType(), param.getMessage());
            return Result.ok(relation);
        } catch (Exception e) {
            return Result.fail(500, "发送匹配请求失败: " + e.getMessage());
        }
    }

    /**
     * 接受匹配请求
     */
    @PostMapping("/accept/{requestId}")
    public Result acceptRequest(@PathVariable Long requestId) {
        try {
            PairRelation relation = partnerMatchingService.acceptRequest(requestId);
            return Result.ok(relation);
        } catch (Exception e) {
            return Result.fail(500, "接受匹配失败: " + e.getMessage());
        }
    }

    /**
     * 拒绝匹配请求
     */
    @PostMapping("/reject/{requestId}")
    public Result rejectRequest(@PathVariable Long requestId) {
        try {
            PairRelation relation = partnerMatchingService.rejectRequest(requestId);
            return Result.ok(relation);
        } catch (Exception e) {
            return Result.fail(500, "拒绝匹配失败: " + e.getMessage());
        }
    }

    /**
     * 获取已匹配的伙伴列表
     */
    @GetMapping("/matches/{userId}")
    public Result getMatches(@PathVariable Long userId) {
        try {
            var matches = partnerMatchingService.getMyMatches(userId);
            return Result.ok(matches);
        } catch (Exception e) {
            return Result.fail(500, "获取伙伴列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取收到的待处理请求
     */
    @GetMapping("/pending/{userId}")
    public Result getPendingRequests(@PathVariable Long userId) {
        try {
            List<PairRelation> requests = partnerMatchingService.getPendingRequests(userId);
            return Result.ok(requests);
        } catch (Exception e) {
            return Result.fail(500, "获取待处理请求失败: " + e.getMessage());
        }
    }

    /**
     * 获取发出的待处理请求
     */
    @GetMapping("/sent/{userId}")
    public Result getSentRequests(@PathVariable Long userId) {
        try {
            List<PairRelation> requests = partnerMatchingService.getSentRequests(userId);
            return Result.ok(requests);
        } catch (Exception e) {
            return Result.fail(500, "获取已发送请求失败: " + e.getMessage());
        }
    }

    /**
     * 取消发出的请求
     */
    @PostMapping("/cancel/{requestId}")
    public Result cancelRequest(@PathVariable Long requestId, @RequestParam Long userId) {
        try {
            partnerMatchingService.cancelRequest(requestId, userId);
            return Result.ok("请求已取消");
        } catch (Exception e) {
            return Result.fail(500, "取消请求失败: " + e.getMessage());
        }
    }

    /**
     * 解除匹配
     */
    @PostMapping("/unmatch/{relationId}")
    public Result unmatch(@PathVariable Long relationId, @RequestParam Long userId) {
        try {
            partnerMatchingService.unmatch(relationId, userId);
            return Result.ok("已解除匹配");
        } catch (Exception e) {
            return Result.fail(500, "解除匹配失败: " + e.getMessage());
        }
    }

    // ==================== 请求参数 ====================

    public static class SendRequestParam {
        private Long fromUserId;
        private Long toUserId;
        private String matchType;
        private String message;

        public Long getFromUserId() { return fromUserId; }
        public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }
        public Long getToUserId() { return toUserId; }
        public void setToUserId(Long toUserId) { this.toUserId = toUserId; }
        public String getMatchType() { return matchType; }
        public void setMatchType(String matchType) { this.matchType = matchType; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
