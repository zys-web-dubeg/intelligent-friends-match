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
@TableName("team_member_relation")
public class TeamMemberRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long teamId;
    
    private Long userId;
    
    private String role; // 成员角色
}