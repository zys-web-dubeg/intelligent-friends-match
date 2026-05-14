CREATE TABLE IF NOT EXISTS `user_profile` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `mbti_type` VARCHAR(10) DEFAULT NULL COMMENT 'MBTI性格类型',
    `tags` TEXT DEFAULT NULL COMMENT '用户标签，JSON格式存储',
    `interests` TEXT DEFAULT NULL COMMENT '兴趣爱好',
    `preferences` TEXT DEFAULT NULL COMMENT '偏好设置',
    `personality_traits` TEXT DEFAULT NULL COMMENT '性格特征',
    `communication_style` VARCHAR(255) DEFAULT NULL COMMENT '沟通风格',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_mbti_type` (`mbti_type`),
    CONSTRAINT `fk_user_profile_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户画像表';
