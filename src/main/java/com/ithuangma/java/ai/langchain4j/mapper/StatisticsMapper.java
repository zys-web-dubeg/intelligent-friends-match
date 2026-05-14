package com.ithuangma.java.ai.langchain4j.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ithuangma.java.ai.langchain4j.entity.Statistics;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface StatisticsMapper extends BaseMapper<Statistics> {
    
    /**
     * 获取指定时间段内的统计数据
     */
    List<Map<String, Object>> selectByDateRange(
            @Param("statisticsType") String statisticsType,
            @Param("apiType") String apiType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * 按日期汇总统计数据
     */
    List<Map<String, Object>> selectByDateGroup(
            @Param("statisticsType") String statisticsType,
            @Param("apiType") String apiType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}