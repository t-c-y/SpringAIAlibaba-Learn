package com.example.springaialibaba.orderagent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 教学用的“用户上下文”。真实项目中来自网关 / SecurityContext / RequestScope。
 *
 * 这里用 ThreadLocal，Controller 在每次请求进来时 set，工具方法执行时 get。
 * 关键教学意义：把“当前登录用户”作为 Guard，防止大模型在参数里塞别人的 userId。
 */
public final class UserContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    /** 模拟数据源：userId -> 允许查看的订单集合。 */
    public static final Map<String, List<Order>> ORDERS = new ConcurrentHashMap<>(Map.of(
            "u001", List.of(
                    new Order("O-1001", "u001", "MacBook Pro 14", new BigDecimal("15999.00"),
                            LocalDate.of(2026, 6, 20), "已发货"),
                    new Order("O-1002", "u001", "AirPods Pro", new BigDecimal("1899.00"),
                            LocalDate.of(2026, 6, 25), "已签收")
            ),
            "u002", List.of(
                    new Order("O-2001", "u002", "iPad Air", new BigDecimal("4799.00"),
                            LocalDate.of(2026, 6, 28), "已支付")
            )
    ));

    private UserContext() {}
    public static void set(String userId) { CURRENT.set(userId); }
    public static String get() { return CURRENT.get(); }
    public static void clear() { CURRENT.remove(); }

    public record Order(String orderId, String ownerUserId, String itemName,
                        BigDecimal totalAmount, LocalDate paidAt, String status) {}
}
