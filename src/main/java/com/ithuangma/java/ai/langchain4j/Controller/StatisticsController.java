package com.ithuangma.java.ai.langchain4j.Controller;

import com.ithuangma.java.ai.langchain4j.Bean.Result;
import com.ithuangma.java.ai.langchain4j.Service.StatisticsService;
import com.ithuangma.java.ai.langchain4j.dto.ChartDataDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@Tag(name = "数据统计")
@RequestMapping("/api/statistics")
public class StatisticsController {
    
    @Autowired
    private StatisticsService statisticsService;
    
    @Operation(summary = "获取用户注册统计")
    @GetMapping("/user-register")
    public Result<ChartDataDto> getUserRegistrationStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ChartDataDto data = statisticsService.getUserRegistrationStats(startDate, endDate);
        return Result.ok(data);
    }
    
    @Operation(summary = "获取API请求统计")
    @GetMapping("/api-request")
    public Result<ChartDataDto> getApiRequestStats(
            @RequestParam("apiType") String apiType,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ChartDataDto data = statisticsService.getApiRequestStats(apiType, startDate, endDate);
        return Result.ok(data);
    }
    
    @Operation(summary = "获取所有API请求统计")
    @GetMapping("/all-api-requests")
    public Result<ChartDataDto> getAllApiRequestsStats(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ChartDataDto data = statisticsService.getAllApiRequestsStats(startDate, endDate);
        return Result.ok(data);
    }
    
    @Operation(summary = "获取用户注册与API请求对比")
    @GetMapping("/comparison")
    public Result<ChartDataDto> getUserAndApiComparisonStats(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ChartDataDto data = statisticsService.getUserAndApiComparisonStats(startDate, endDate);
        return Result.ok(data);
    }
    
    @Operation(summary = "获取近3天统计数据")
    @GetMapping("/recent-three-days")
    public Result<Map<String, Object>> getRecentThreeDaysStats() {
        Map<String, Object> data = statisticsService.getRecentThreeDaysStats();
        return Result.ok(data);
    }
    
    @Operation(summary = "获取近7天统计数据")
    @GetMapping("/recent-seven-days")
    public Result<Map<String, Object>> getRecentSevenDaysStats() {
        Map<String, Object> data = statisticsService.getRecentSevenDaysStats();
        return Result.ok(data);
    }
    
    @Operation(summary = "获取近30天统计数据")
    @GetMapping("/recent-thirty-days")
    public Result<Map<String, Object>> getRecentThirtyDaysStats() {
        Map<String, Object> data = statisticsService.getRecentThirtyDaysStats();
        return Result.ok(data);
    }
}