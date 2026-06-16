create table shopping_order
(
    id           bigint auto_increment
        primary key,
    username     varchar(100)                  not null comment '收货人姓名',
    phone        varchar(20)                   not null comment '联系电话',
    address      varchar(500)                  not null comment '收货地址',
    product_name varchar(200)                  not null comment '商品名称',
    quantity     int                           not null comment '数量',
    total_price  decimal(10, 2)                not null comment '总价',
    status       varchar(20) default 'PENDING' not null comment '订单状态',
    create_time  datetime                      not null comment '创建时间'
)
    comment '购物订单表';

