CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名',
    `password` VARCHAR(128) NOT NULL COMMENT '密码',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
INSERT INTO guiguxiaozhi.sys_user (id, username, password, phone, email, create_time, update_time) VALUES (1, '邹益松', 'd43b7bb972b6a555a78499c4d07a8c98', '19570126030', 'modiyouyang@qq.com', '2026-04-29 09:42:55', '2026-05-19 10:23:07');
INSERT INTO guiguxiaozhi.sys_user (id, username, password, phone, email, create_time, update_time) VALUES (7, '符传翔', 'd43b7bb972b6a555a78499c4d07a8c98', '17681791939', '1191151948@qq.com', '2026-05-07 08:46:54', '2026-05-19 10:23:07');
INSERT INTO guiguxiaozhi.sys_user (id, username, password, phone, email, create_time, update_time) VALUES (8, '章志康', 'd43b7bb972b6a555a78499c4d07a8c98', '19725171396', '3163332402@qq.com', '2026-05-18 17:46:15', '2026-05-18 17:46:15');
INSERT INTO guiguxiaozhi.sys_user (id, username, password, phone, email, create_time, update_time) VALUES (9, '涂志兴', 'd43b7bb972b6a555a78499c4d07a8c98', '15727515819', '3420964887@qq.com', '2026-05-18 19:51:36', '2026-05-18 19:51:36');
INSERT INTO guiguxiaozhi.sys_user (id, username, password, phone, email, create_time, update_time) VALUES (10, '卢琦', 'd43b7bb972b6a555a78499c4d07a8c98', '17876563724', '1787656372@qq.com', '2026-05-18 19:56:39', '2026-05-18 19:56:39');
INSERT INTO guiguxiaozhi.sys_user (id, username, password, phone, email, create_time, update_time) VALUES (11, '王在辉', 'd43b7bb972b6a555a78499c4d07a8c98', '15279065601', '1456093048@qq.com', '2026-05-18 20:06:38', '2026-05-18 20:06:38');
INSERT INTO guiguxiaozhi.sys_user (id, username, password, phone, email, create_time, update_time) VALUES (12, '晏先敏', 'd43b7bb972b6a555a78499c4d07a8c98', '18720931937', '1441799027@qq.com', '2026-05-18 20:18:32', '2026-05-18 20:18:32');
INSERT INTO guiguxiaozhi.sys_user (id, username, password, phone, email, create_time, update_time) VALUES (13, '陈俊辉', 'd43b7bb972b6a555a78499c4d07a8c98', '19179477260', '2012199344@qq.com', '2026-05-18 20:23:53', '2026-05-18 20:23:53');
INSERT INTO guiguxiaozhi.sys_user (id, username, password, phone, email, create_time, update_time) VALUES (14, '祝雍健', 'd43b7bb972b6a555a78499c4d07a8c98', '15478963254', '2336964047@qq.com', '2026-05-19 09:51:04', '2026-05-19 09:51:04');
INSERT INTO guiguxiaozhi.sys_user (id, username, password, phone, email, create_time, update_time) VALUES (15, 'r__way', '09d69fac4944aeca634c962717b3c61c', '18988544064', '2426880072@qq.com', '2026-06-05 19:16:38', '2026-06-05 19:16:38');

