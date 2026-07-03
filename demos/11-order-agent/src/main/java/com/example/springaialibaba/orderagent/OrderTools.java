package com.example.springaialibaba.orderagent;

import com.example.springaialibaba.orderagent.UserContext.Order;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 订单工具类。业务型 Tool 与 09/10 课的差异：
 *
 * 1. 工具方法内部一律从 UserContext 取 currentUserId，而不是让模型把它写进参数。
 * 2. 涉及跨用户的操作直接返回“无权限”，不要抛异常，模型据此生成友好话术。
 * 3. 所有工具都是“只读查询”，不做写操作；写操作应该走人工确认 + 幂等接口，本课不引入。
 */
@Component
public class OrderTools {

    @Tool(description = "列出当前登录用户的所有订单，返回 orderId、商品、金额、状态。仅返回本人订单。")
    public List<Order> listMyOrders() {
        String uid = UserContext.get();
        if (uid == null) return List.of();
        return UserContext.ORDERS.getOrDefault(uid, List.of());
    }

    @Tool(description = """
            按订单号查看订单详情。若订单不存在或不属于当前登录用户，均返回 allowed=false。
            禁止用于修改订单，只用于查询。
            """)
    public Map<String, Object> getOrderById(
            @ToolParam(description = "订单编号，例如 O-1001") String orderId) {
        String uid = UserContext.get();
        if (uid == null) return Map.of("allowed", false, "reason", "not authenticated");

        Optional<Order> found = UserContext.ORDERS.values().stream()
                .flatMap(List::stream)
                .filter(o -> o.orderId().equalsIgnoreCase(orderId))
                .findFirst();

        if (found.isEmpty()) return Map.of("allowed", false, "reason", "order not found: " + orderId);
        Order o = found.get();
        if (!o.ownerUserId().equals(uid)) {
            // 关键：越权访问不返回订单内容，只回“无权限”。这条边界必须在工具层守住，不能靠 Prompt 约束。
            return Map.of("allowed", false, "reason", "you do not own this order");
        }
        return Map.of("allowed", true, "order", o);
    }
}
