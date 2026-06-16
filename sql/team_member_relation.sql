create table team_member_relation
(
    id        bigint auto_increment comment '主键ID'
        primary key,
    team_id   bigint                                not null comment '队伍ID',
    user_id   bigint                                not null comment '用户ID',
    role      varchar(50) default 'member'          null comment '成员角色',
    joined_at timestamp   default CURRENT_TIMESTAMP null comment '加入时间',
    constraint uk_team_user
        unique (team_id, user_id),
    constraint team_member_relation_ibfk_1
        foreign key (team_id) references team_profile (id)
            on delete cascade
)
    comment '队伍成员关系表' collate = utf8mb4_unicode_ci;

create index idx_team_user
    on team_member_relation (team_id, user_id);

create index idx_user_id
    on team_member_relation (user_id);
INSERT INTO guiguxiaozhi.team_member_relation (id, team_id, user_id, role, joined_at) VALUES (9, 1, 1, 'member', '2026-05-10 16:08:37');
INSERT INTO guiguxiaozhi.team_member_relation (id, team_id, user_id, role, joined_at) VALUES (10, 3, 10, 'member', '2026-05-18 20:02:30');
INSERT INTO guiguxiaozhi.team_member_relation (id, team_id, user_id, role, joined_at) VALUES (11, 1, 11, 'member', '2026-05-18 20:15:53');
INSERT INTO guiguxiaozhi.team_member_relation (id, team_id, user_id, role, joined_at) VALUES (12, 1, 12, 'member', '2026-05-18 20:22:10');
INSERT INTO guiguxiaozhi.team_member_relation (id, team_id, user_id, role, joined_at) VALUES (13, 1, 13, 'member', '2026-05-18 20:26:21');
INSERT INTO guiguxiaozhi.team_member_relation (id, team_id, user_id, role, joined_at) VALUES (14, 3, 13, 'member', '2026-05-18 20:26:54');
INSERT INTO guiguxiaozhi.team_member_relation (id, team_id, user_id, role, joined_at) VALUES (15, 3, 14, 'member', '2026-05-19 10:01:58');
INSERT INTO guiguxiaozhi.team_member_relation (id, team_id, user_id, role, joined_at) VALUES (16, 3, 8, 'member', '2026-05-19 12:01:19');
INSERT INTO guiguxiaozhi.team_member_relation (id, team_id, user_id, role, joined_at) VALUES (17, 1, 8, 'member', '2026-05-19 12:01:21');
INSERT INTO guiguxiaozhi.team_member_relation (id, team_id, user_id, role, joined_at) VALUES (18, 1, 15, 'member', '2026-06-05 19:24:46');
