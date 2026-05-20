package com.ithuangma.java.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("user_profile")
public class UserProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String mbtiType; // MBTI类型
    
    private String tags; // 用户标签，JSON格式存储

    private String interests; // 兴趣爱好

    private String preferences; // 偏好设置

    private String personalityTraits; // 性格特征

    private String communicationStyle; // 沟通风格

    private String avatar; // 用户头像URL
}