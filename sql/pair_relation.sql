CREATE TABLE IF NOT EXISTS `pair_relation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `from_user_id` BIGINT NOT NULL COMMENT '发起匹配请求的用户ID',
    `to_user_id` BIGINT NOT NULL COMMENT '接收匹配请求的用户ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending-待接受, accepted-已匹配, rejected-已拒绝, blocked-已屏蔽',
    `match_type` VARCHAR(20) NOT NULL COMMENT '匹配类型: study-学习, hobby-兴趣, sports-运动, game-游戏, friend-交友, date-恋爱',
    `message` VARCHAR(500) DEFAULT NULL COMMENT '匹配请求附言',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_from_user_id` (`from_user_id`),
    KEY `idx_to_user_id` (`to_user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_match_type` (`match_type`),
    UNIQUE KEY `uk_from_to_type` (`from_user_id`, `to_user_id`, `match_type`),
    CONSTRAINT `fk_pair_from_user` FOREIGN KEY (`from_user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_pair_to_user` FOREIGN KEY (`to_user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='伙伴匹配关系表';
