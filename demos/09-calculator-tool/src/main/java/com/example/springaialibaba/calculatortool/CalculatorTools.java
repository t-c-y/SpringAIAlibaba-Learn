package com.example.springaialibaba.calculatortool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 计算器工具类。
 *
 * 教学要点：
 * 1. 方法上加 @Tool，方法参数上加 @ToolParam，注解上的 description 会成为模型选择工具的依据。
 * 2. Spring AI 会读取方法签名 + 注解生成 JSON Schema，把可用工具描述在 System Prompt 里告诉模型。
 * 3. 工具方法必须"语义清晰、单一职责、参数命名与描述一致"，模型才更容易选对工具、传对参数。
 * 4. 真实项目里，每个工具方法都应该打印入参和返回结果（以及异常），方便排查"模型调了什么工具、传了什么参数、返回什么给模型"。
 *    Tool Calling 是一个多轮、不可见的循环（模型→选工具→代码执行→结果回传模型），没有日志几乎无法调试。
 *
 * 常见误区：不要把复杂业务塞进一个工具方法。宁可拆成 add / sub / mul / div 四个小工具，
 * 也不要写一个大而全的 "compute(expression)"，那样模型很容易乱选或参数格式错误。
 */
@Component
public class CalculatorTools {

    private static final Logger log = LoggerFactory.getLogger(CalculatorTools.class);

    @Tool(description = "两个数相加，输入 a 和 b，返回 a + b。")
    public double add(@ToolParam(description = "第一个加数") double a,
                      @ToolParam(description = "第二个加数") double b) {
        log.info("[CalculatorTool] add called, a={}, b={}", a, b);
        double result = a + b;
        log.info("[CalculatorTool] add result = {} (a={}, b={})", result, a, b);
        return result;
    }

    @Tool(description = "两个数相减，返回 a - b。")
    public double subtract(@ToolParam(description = "被减数") double a,
                           @ToolParam(description = "减数") double b) {
        log.info("[CalculatorTool] subtract called, a={}, b={}", a, b);
        double result = a - b;
        log.info("[CalculatorTool] subtract result = {} (a={}, b={})", result, a, b);
        return result;
    }

    @Tool(description = "两个数相乘，返回 a * b。")
    public double multiply(@ToolParam(description = "第一个乘数") double a,
                           @ToolParam(description = "第二个乘数") double b) {
        log.info("[CalculatorTool] multiply called, a={}, b={}", a, b);
        double result = a * b;
        log.info("[CalculatorTool] multiply result = {} (a={}, b={})", result, a, b);
        return result;
    }

    @Tool(description = "两个数相除，返回 a / b。除数为 0 时抛异常并向调用方返回原因。")
    public double divide(@ToolParam(description = "被除数") double a,
                         @ToolParam(description = "除数，不能为 0") double b) {
        log.info("[CalculatorTool] divide called, a={}, b={}", a, b);
        if (b == 0) {
            log.warn("[CalculatorTool] divide failed: 除数不能为 0, a={}", a);
            throw new IllegalArgumentException("除数不能为 0");
        }
        double result = a / b;
        log.info("[CalculatorTool] divide result = {} (a={}, b={})", result, a, b);
        return result;
    }
}
