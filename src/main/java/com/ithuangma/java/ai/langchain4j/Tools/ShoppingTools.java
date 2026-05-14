package com.ithuangma.java.ai.langchain4j.Tools;

import com.ithuangma.java.ai.langchain4j.Service.ShoppingOrderService;
import com.ithuangma.java.ai.langchain4j.entity.ShoppingOrder;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class ShoppingTools {

    @Autowired
    private ShoppingOrderService shoppingOrderService;

    @Tool(name = "创建订单", value = "根据用户提供的商品名称、数量、总价、收货人姓名、电话和地址创建订单，返回创建结果")
    public String createOrder(
            @P(value = "商品名称") String productName,
            @P(value = "数量") Integer quantity,
            @P(value = "总价") BigDecimal totalPrice,
            @P(value = "收货人姓名") String username,
            @P(value = "联系电话") String phone,
            @P(value = "收货地址") String address
    ) {
        ShoppingOrder order = new ShoppingOrder();
        order.setProductName(productName);
        order.setQuantity(quantity);
        order.setTotalPrice(totalPrice);
        order.setUsername(username);
        order.setPhone(phone);
        order.setAddress(address);
        order.setStatus("PENDING");
        order.setCreateTime(LocalDateTime.now());

        if (shoppingOrderService.save(order)) {
            return "订单创建成功！订单编号：" + order.getId() + "，请尽快完成支付。";
        } else {
            return "订单创建失败，请稍后重试。";
        }
    }

    @Tool(name = "查询订单", value = "根据订单ID查询订单详情，返回订单信息")
    public String queryOrder(@P(value = "订单ID") Long orderId) {
        ShoppingOrder order = shoppingOrderService.getById(orderId);
        if (order == null) {
            return "未找到该订单，请核对订单编号。";
        }
        return String.format("订单编号：%d\n商品：%s\n数量：%d\n总价：%.2f元\n收货人：%s\n电话：%s\n地址：%s\n状态：%s\n创建时间：%s",
                order.getId(), order.getProductName(), order.getQuantity(),
                order.getTotalPrice(), order.getUsername(), order.getPhone(),
                order.getAddress(), getStatusText(order.getStatus()), order.getCreateTime());
    }

    @Tool(name = "取消订单", value = "根据订单ID取消订单，只有待支付的订单才能取消")
    public String cancelOrder(@P(value = "订单ID") Long orderId) {
        ShoppingOrder order = shoppingOrderService.getById(orderId);
        if (order == null) {
            return "未找到该订单，请核对订单编号。";
        }
        if (!"PENDING".equals(order.getStatus())) {
            return "只有待支付的订单才能取消，当前订单状态为：" + getStatusText(order.getStatus());
        }
        order.setStatus("CANCELLED");
        if (shoppingOrderService.updateById(order)) {
            return "订单已成功取消。";
        }
        return "取消订单失败，请稍后重试。";
    }

    private String getStatusText(String status) {
        switch (status) {
            case "PENDING": return "待支付";
            case "PAID": return "已支付";
            case "SHIPPED": return "已发货";
            case "DELIVERED": return "已送达";
            case "CANCELLED": return "已取消";
            default: return status;
        }
    }
}