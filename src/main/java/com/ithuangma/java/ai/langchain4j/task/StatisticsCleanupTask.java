package com.ithuangma.java.ai.langchain4j.task;

import com.ithuangma.java.ai.langchain4j.mapper.StatisticsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 统计数据清理任务
 */
@Component
public class StatisticsCleanupTask {
    
    @Autowired
    private StatisticsMapper statisticsMapper;
    
    /**
     * 每天凌晨2点执行数据清理任务，删除超过3个月的数据
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldStatistics() {
        // 删除超过3个月的统计数据
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        
        // 注意：实际生产环境中应使用逻辑删除或者更安全的删除方式
        // 这里仅为演示目的
        System.out.println("正在清理超过3个月的统计数据..." + threeMonthsAgo);
    }
}