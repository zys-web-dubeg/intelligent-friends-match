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
INSERT INTO guiguxiaozhi.user_profile (id, user_id, mbti_type, tags, interests, preferences, personality_traits, communication_style, create_time, update_time, avatar) VALUES (1, 1, 'ISFJ', '多情者、热爱生活、情感细腻、独来独往', '马拉松、足球、游戏', '喜欢一个人，重情感细节怕被忽视，偏好情感沟通', '内向、逻辑性强、追求完美、情感丰富', '直接、简洁、注重效率', '2026-05-09 11:11:32', '2026-05-19 10:43:52', 'https://java-huangma.oss-cn-beijing.aliyuncs.com/2026/05/dbfdfb52-19e5-447a-8406-49b86b04d99f.jpg');
INSERT INTO guiguxiaozhi.user_profile (id, user_id, mbti_type, tags, interests, preferences, personality_traits, communication_style, create_time, update_time, avatar) VALUES (6, 8, 'ESFJ', '网络工程师、爱打杂、不卑不亢', '打游戏、躺平', '喜欢协作式工作环境，乐于分享知识，偏爱开发沟通的团队氛围。', '外向、热情、乐于助人、善解人意、充满活力', '直接、友好、鼓励他人、喜欢用积极语言带动气氛', '2026-05-18 19:10:21', '2026-05-19 10:40:20', 'https://java-huangma.oss-cn-beijing.aliyuncs.com/2026/05/e91a95f2-285c-41b1-b451-f3ea67c841b2.jpg');
INSERT INTO guiguxiaozhi.user_profile (id, user_id, mbti_type, tags, interests, preferences, personality_traits, communication_style, create_time, update_time, avatar) VALUES (7, 7, 'INTP', '外卖员、单王、寒假工', '打游戏', '喜欢一个人在寝室打游戏，没有什么运动爱好，喜欢宅', '有时内向有时外向、沉稳、情绪稳定', '直接、感性', '2026-05-18 19:45:50', '2026-05-18 19:45:50', 'https://java-huangma.oss-cn-beijing.aliyuncs.com/2026/05/bba25dd4-29c6-449c-a894-6603e26ce489.jpg');
INSERT INTO guiguxiaozhi.user_profile (id, user_id, mbti_type, tags, interests, preferences, personality_traits, communication_style, create_time, update_time, avatar) VALUES (8, 9, 'ESFP', '咸鱼、摆烂、躺平、享受生活', 'K歌、台球、游戏、泡妞', '喜欢热闹的环境、喜欢和朋友一起到处玩', '外向、开朗、直来直去', '直接、友好、幽默', '2026-05-18 19:54:03', '2026-05-18 19:54:03', 'https://java-huangma.oss-cn-beijing.aliyuncs.com/2026/05/6ceb085f-6c73-4837-bcf2-04f98e2f9333.jpg');
INSERT INTO guiguxiaozhi.user_profile (id, user_id, mbti_type, tags, interests, preferences, personality_traits, communication_style, create_time, update_time, avatar) VALUES (9, 10, 'INFJ', '游戏爱好者、宅、大胃袋', '游戏、自助', '喜欢宅在寝室打游戏、和朋友一起开黑', '外向、情绪稳定、有责任心', '直接、温和、讲条理', '2026-05-18 19:59:04', '2026-05-24 16:30:52', 'https://java-huangma.oss-cn-beijing.aliyuncs.com/2026/05/4035ca28-2388-47ac-8e18-b652a07954d7.jpg');
INSERT INTO guiguxiaozhi.user_profile (id, user_id, mbti_type, tags, interests, preferences, personality_traits, communication_style, create_time, update_time, avatar) VALUES (10, 11, 'INFP', '美剧爱好者、英语能手、喜欢看综艺', '综艺、美剧、音乐、英语', '喜欢安静的环境、一个人刷剧听歌学习', '内向、慷慨、自我', '间接、情感、简洁', '2026-05-18 20:11:11', '2026-05-18 20:11:11', 'https://java-huangma.oss-cn-beijing.aliyuncs.com/2026/05/c924cf29-f4a1-4ef5-9df5-5189dd3f8c6d.jpg');
INSERT INTO guiguxiaozhi.user_profile (id, user_id, mbti_type, tags, interests, preferences, personality_traits, communication_style, create_time, update_time, avatar) VALUES (11, 12, 'ISTP', '喜欢踢足球、前锋、进球', '足球、游戏、学习', '喜欢和别人一起踢球，一起打游戏', '内向、随大流、没主见', '犹豫不决、不坚定、容易受外界影响', '2026-05-18 20:21:29', '2026-05-18 20:21:29', 'https://java-huangma.oss-cn-beijing.aliyuncs.com/2026/05/d38fd542-d563-44c3-ac35-22a60edb4560.jpg');
INSERT INTO guiguxiaozhi.user_profile (id, user_id, mbti_type, tags, interests, preferences, personality_traits, communication_style, create_time, update_time, avatar) VALUES (12, 13, 'INTP', '宅男、刷视频、抽象', '刷视频、踢足球、搞抽象', '喜欢一个人在家刷视频、喜欢一个人做任何事情', '内向、细心、情绪稳定', '直接、友好、单纯', '2026-05-18 20:26:05', '2026-05-18 20:26:05', 'https://java-huangma.oss-cn-beijing.aliyuncs.com/2026/05/d9e0298d-fb8d-42ee-9ba3-136f00e0be97.jpg');
INSERT INTO guiguxiaozhi.user_profile (id, user_id, mbti_type, tags, interests, preferences, personality_traits, communication_style, create_time, update_time, avatar) VALUES (13, 14, 'ESFP', '无业游民，懒蛋，吃喝玩乐', '打游戏，泡妞，美食', '喜欢半夜一个人打游戏，哄人一流', '外向、直白、开放', '直接、强制、pua', '2026-05-19 09:57:51', '2026-05-19 09:57:51', 'https://java-huangma.oss-cn-beijing.aliyuncs.com/2026/05/9aac46c8-3f89-486d-a1d3-b74e8b1044e2.jpg');
INSERT INTO guiguxiaozhi.user_profile (id, user_id, mbti_type, tags, interests, preferences, personality_traits, communication_style, create_time, update_time, avatar) VALUES (14, 15, 'INFP', 'inf power', '123', '123', '123', '123', '2026-06-05 19:22:54', '2026-06-05 19:31:52', '');

