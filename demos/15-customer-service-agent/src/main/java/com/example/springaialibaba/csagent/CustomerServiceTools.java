package com.example.springaialibaba.csagent;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 客服业务工具。教学阶段用内存数据，字段命名和真实电商保持一致。
 *
 * 边界：只做只读查询 + 一个受限的“退款申请”动作，且退款需要人工确认（返回 pending，让上层展示确认弹窗）。
 */
@Component
public class CustomerServiceTools {

    private static final Map<String, Order> ORDERS = Map.of(
            "O-1001", new Order("O-1001", "u001", "MacBook Pro 14", 15999.00,
                    LocalDate.of(2026, 6, 20), "已发货"),
            "O-1002", new Order("O-1002", "u001", "AirPods Pro", 1899.00,
                    LocalDate.of(2026, 6, 25), "已签收"),
            "O-2001", new Order("O-2001", "u002", "iPad Air", 4799.00,
                    LocalDate.of(2026, 6, 28), "已支付"));

    @Tool(description = "查询当前用户的所有订单，仅返回本人订单。")
    public List<Order> listMyOrders(@ToolParam(description = "当前登录用户 id，由服务端注入，不由用户填") String currentUserId) {
        return ORDERS.values().stream().filter(o -> o.ownerUserId().equals(currentUserId)).toList();
    }

    @Tool(description = "按订单号查询详情，非本人订单返回 allowed=false。")
    public Map<String, Object> getOrder(@ToolParam(description = "订单号，例如 O-1001") String orderId,
                                        @ToolParam(description = "当前登录用户 id") String currentUserId) {
        Order o = ORDERS.get(orderId);
        if (o == null) return Map.of("allowed", false, "reason", "order not found");
        if (!o.ownerUserId().equals(currentUserId)) return Map.of("allowed", false, "reason", "not owner");
        return Map.of("allowed", true, "order", o);
    }

    @Tool(description = """
            对某订单发起退款申请。此工具不会真正退款，而是登记一条待人工确认的申请，返回 status=pending。
            调用者必须在最终回答里明确告知用户"退款申请已提交，等待客服确认"。
            """)
    public Map<String, Object> requestRefund(@ToolParam(description = "订单号") String orderId,
                                             @ToolParam(description = "退款理由") String reason,
                                             @ToolParam(description = "当前登录用户 id") String currentUserId) {
        Optional<Order> o = Optional.ofNullable(ORDERS.get(orderId));
        if (o.isEmpty() || !o.get().ownerUserId().equals(currentUserId)) {
            return Map.of("allowed", false, "reason", "no permission");
        }
        return Map.of("allowed", true, "status", "pending",
                "orderId", orderId, "reason", reason,
                "message", "退款申请已提交，等待客服人工确认。");
    }

    public record Order(String orderId, String ownerUserId, String itemName,
                        double totalAmount, LocalDate paidAt, String status) {}
}
