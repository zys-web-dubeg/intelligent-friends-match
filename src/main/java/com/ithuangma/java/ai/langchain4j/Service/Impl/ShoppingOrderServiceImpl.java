package com.ithuangma.java.ai.langchain4j.Service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ithuangma.java.ai.langchain4j.entity.ShoppingOrder;
import com.ithuangma.java.ai.langchain4j.mapper.ShoppingOrderMapper;
import com.ithuangma.java.ai.langchain4j.Service.ShoppingOrderService;
import org.springframework.stereotype.Service;

@Service
public class ShoppingOrderServiceImpl extends ServiceImpl<ShoppingOrderMapper, ShoppingOrder> implements ShoppingOrderService {
}