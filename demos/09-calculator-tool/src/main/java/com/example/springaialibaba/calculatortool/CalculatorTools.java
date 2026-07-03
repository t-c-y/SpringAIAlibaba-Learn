package com.example.springaialibaba.calculatortool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 计算器工具类。
 *
 * 教学要点：
 * 1. 方法上加 @Tool，方法参数上加 @ToolParam，注解上的 description 会成为模型选择工具的依据。
 * 2. Spring AI 会读取方法签名 + 注解生成 JSON Schema，把可用工具描述在 System Prompt 里告诉模型。
 * 3. 工具方法必须“语义清晰、单一职责、参数命名与描述一致”，模型才更容易选对工具、传对参数。
 *
 * 常见误区：不要把复杂业务塞进一个工具方法。宁可拆成 add / sub / mul / div 四个小工具，
 * 也不要写一个大而全的 "compute(expression)"，那样模型很容易乱选或参数格式错误。
 */
@Component
public class CalculatorTools {

    @Tool(description = "两个数相加，输入 a 和 b，返回 a + b。")
    public double add(@ToolParam(description = "第一个加数") double a,
                      @ToolParam(description = "第二个加数") double b) {
        return a + b;
    }

    @Tool(description = "两个数相减，返回 a - b。")
    public double subtract(@ToolParam(description = "被减数") double a,
                           @ToolParam(description = "减数") double b) {
        return a - b;
    }

    @Tool(description = "两个数相乘，返回 a * b。")
    public double multiply(@ToolParam(description = "第一个乘数") double a,
                           @ToolParam(description = "第二个乘数") double b) {
        return a * b;
    }

    @Tool(description = "两个数相除，返回 a / b。除数为 0 时抛异常并向调用方返回原因。")
    public double divide(@ToolParam(description = "被除数") double a,
                         @ToolParam(description = "除数，不能为 0") double b) {
        if (b == 0) {
            throw new IllegalArgumentException("除数不能为 0");
        }
        return a / b;
    }
}
