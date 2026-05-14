package com.ithuangma.java.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("team_profile")
public class TeamProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String pineconeVectorId;
    
    private String personalityConstraint; // 个性约束条件
    
    private Integer accessLevel; // 访问级别，0-公开，1-审核，2-私有
}