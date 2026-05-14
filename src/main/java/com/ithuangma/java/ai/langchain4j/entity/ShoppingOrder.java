package com.ithuangma.java.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("shopping_order")
public class ShoppingOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String phone;
    private String address;
    private String productName;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String status; // PENDING-待支付, PAID-已支付, SHIPPED-已发货, DELIVERED-已送达, CANCELLED-已取消
    private LocalDateTime createTime;
}