create table appointment
(
    id          bigint auto_increment
        primary key,
    username    varchar(50) not null,
    id_card     varchar(18) not null,
    department  varchar(50) not null,
    date        varchar(10) not null,
    time        varchar(10) not null,
    doctor_name varchar(50) null
);
INSERT INTO guiguxiaozhi.appointment (id, username, id_card, department, date, time, doctor_name) VALUES (12, '陈俊辉', '362527200507181716', '内科', '2026-04-15', '上午', '张明远');
INSERT INTO guiguxiaozhi.appointment (id, username, id_card, department, date, time, doctor_name) VALUES (13, '邹义松', '362527200407163313', '神经内科', '2026-04-16', '上午', '章医生');
