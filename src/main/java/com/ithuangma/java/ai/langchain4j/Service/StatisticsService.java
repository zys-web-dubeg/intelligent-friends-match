package com.ithuangma.java.ai.langchain4j.Service;

import com.ithuangma.java.ai.langchain4j.dto.ChartDataDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface StatisticsService {
    
    /**
     * 记录用户注册统计
     */
    void recordUserRegistration();
    
    /**
     * 记录API请求统计
     */
    void recordApiRequest(String apiType);
    
    /**
     * 获取用户注册统计数据
     */
    ChartDataDto getUserRegistrationStats(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取API请求统计数据
     */
    ChartDataDto getApiRequestStats(String apiType, LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取所有API类型的请求统计数据
     */
    ChartDataDto getAllApiRequestsStats(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取用户注册和API请求对比数据
     */
    ChartDataDto getUserAndApiComparisonStats(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取近3天统计数据
     */
    Map<String, Object> getRecentThreeDaysStats();
    
    /**
     * 获取近7天统计数据
     */
    Map<String, Object> getRecentSevenDaysStats();
    
    /**
     * 获取近30天统计数据
     */
    Map<String, Object> getRecentThirtyDaysStats();
}