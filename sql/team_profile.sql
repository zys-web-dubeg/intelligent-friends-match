create table team_profile
(
    id                     bigint auto_increment comment '主键ID'
        primary key,
    name                   varchar(255)                        not null comment '队伍名称',
    pinecone_vector_id     varchar(255)                        null comment '对应Pinecone中的向量ID',
    personality_constraint text                                null comment '个性约束条件，用于匹配',
    access_level           int       default 0                 null comment '访问级别，0-公开，1-审核，2-私有',
    created_at             timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at             timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '队伍基础信息表' collate = utf8mb4_unicode_ci;

create index idx_access_level
    on team_profile (access_level);

create index idx_created_at
    on team_profile (created_at);
INSERT INTO guiguxiaozhi.team_profile (id, name, pinecone_vector_id, personality_constraint, access_level, created_at, updated_at) VALUES (1, '学习互助小组', 'vec_study_group_001', '学习型人格', 0, '2026-05-08 09:29:35', '2026-05-08 09:29:35');
INSERT INTO guiguxiaozhi.team_profile (id, name, pinecone_vector_id, personality_constraint, access_level, created_at, updated_at) VALUES (2, '情感交流圈子', 'vec_emotion_circle_002', '情感支持型', 1, '2026-05-08 09:29:35', '2026-05-08 09:29:35');
INSERT INTO guiguxiaozhi.team_profile (id, name, pinecone_vector_id, personality_constraint, access_level, created_at, updated_at) VALUES (3, '购物分享团队', 'vec_shopping_team_003', '购物消费导向', 0, '2026-05-08 09:29:35', '2026-05-08 09:29:35');
INSERT INTO guiguxiaozhi.team_profile (id, name, pinecone_vector_id, personality_constraint, access_level, created_at, updated_at) VALUES (4, '兴趣爱好联盟', 'vec_hobby_union_004', '兴趣导向型', 2, '2026-05-09 11:43:12', '2026-05-09 15:06:20');
INSERT INTO guiguxiaozhi.team_profile (id, name, pinecone_vector_id, personality_constraint, access_level, created_at, updated_at) VALUES (2052957532049760259, '001', null, null, 0, '2026-06-05 19:22:00', '2026-06-05 19:22:00');
INSERT INTO guiguxiaozhi.team_profile (id, name, pinecone_vector_id, personality_constraint, access_level, created_at, updated_at) VALUES (2052957532049760260, '002', null, null, 0, '2026-06-05 19:22:19', '2026-06-05 19:22:19');
INSERT INTO guiguxiaozhi.team_profile (id, name, pinecone_vector_id, personality_constraint, access_level, created_at, updated_at) VALUES (2052957532049760261, '003', '123', null, 0, '2026-06-05 19:22:32', '2026-06-05 19:22:32');
INSERT INTO guiguxiaozhi.team_profile (id, name, pinecone_vector_id, personality_constraint, access_level, created_at, updated_at) VALUES (2052957532049760262, '123', '123123', null, 0, '2026-06-05 19:30:48', '2026-06-05 19:30:48');
