package com.ithuangma.java.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("statistics")
public class Statistics {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    // 统计类型: user_register(用户注册), api_request(接口请求)
    private String statisticsType;
    
    // 接口类型: learning, love, shopping, xiaozhi
    private String apiType;
    
    // 统计日期
    private LocalDateTime statisticsDate;
    
    // 统计数值
    private Integer count;
    
    // 创建时间
    private LocalDateTime createTime;
    
    // 更新时间
    private LocalDateTime updateTime;
}