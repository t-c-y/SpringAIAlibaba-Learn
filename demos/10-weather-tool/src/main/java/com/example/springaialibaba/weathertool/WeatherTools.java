package com.example.springaialibaba.weathertool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 天气工具（教学 Mock）。
 *
 * 真实项目会调用 HeWeather / OpenWeather / 内部气象服务。这里为了让课程离线可跑，
 * 用一份静态 Mock。核心教学意义：让你专注在“Tool 描述 + 参数设计 + 错误处理”这三件事，
 * 不被外部 API Key / 网络问题干扰。
 *
 * 生产迁移只需把 CITY_MOCK 替换成 RestClient 调用，方法签名和 @Tool 描述保持不变。
 */
@Component
public class WeatherTools {

    /** 城市代码 -> 天气快照。真实实现里替换成 HTTP 调用。 */
    private static final Map<String, Snapshot> CITY_MOCK = Map.of(
            "beijing", new Snapshot("北京", 18, "多云", 45, 3),
            "shanghai", new Snapshot("上海", 22, "小雨", 78, 4),
            "hangzhou", new Snapshot("杭州", 24, "阴", 65, 2),
            "shenzhen", new Snapshot("深圳", 28, "晴", 55, 3)
    );

    @Tool(description = """
            查询指定城市当前天气。适用于用户询问“现在的天气 / 温度 / 湿度”等实时信息。
            city 需要使用城市英文小写拼音，例如 beijing / shanghai / hangzhou / shenzhen。
            如果城市未支持，会返回 supported=false 让上层告知用户。
            """)
    public CurrentWeather currentWeather(
            @ToolParam(description = "城市英文小写拼音，例如 beijing") String city) {
        Snapshot s = CITY_MOCK.get(city == null ? "" : city.toLowerCase());
        if (s == null) {
            return new CurrentWeather(false, city, null, null, null, null, null,
                    "unsupported city: " + city + "; supported=" + CITY_MOCK.keySet());
        }
        return new CurrentWeather(true, city, s.displayName(), s.temperatureC(),
                s.condition(), s.humidityPercent(), s.windLevel(), null);
    }

    @Tool(description = """
            查询指定城市未来 3 天天气预报。适用于“最近几天 / 周末 / 出行安排”等场景。
            city 使用英文小写拼音；返回值内 date 使用 ISO 格式 yyyy-MM-dd。
            """)
    public ForecastResult forecast(
            @ToolParam(description = "城市英文小写拼音，例如 beijing") String city) {
        Snapshot s = CITY_MOCK.get(city == null ? "" : city.toLowerCase());
        if (s == null) {
            return new ForecastResult(false, city, List.of(),
                    "unsupported city: " + city + "; supported=" + CITY_MOCK.keySet());
        }
        LocalDate today = LocalDate.of(2026, 7, 3); // Mock 用固定日期，避免 UT 时间波动
        List<ForecastDay> days = List.of(
                new ForecastDay(today.toString(), s.temperatureC() - 1, s.temperatureC() + 3, s.condition()),
                new ForecastDay(today.plusDays(1).toString(), s.temperatureC(), s.temperatureC() + 4, "多云"),
                new ForecastDay(today.plusDays(2).toString(), s.temperatureC() - 2, s.temperatureC() + 2, "阴")
        );
        return new ForecastResult(true, city, days, null);
    }

    private record Snapshot(String displayName, int temperatureC, String condition,
                            int humidityPercent, int windLevel) {}

    public record CurrentWeather(boolean supported, String cityCode, String cityName,
                                 Integer temperatureC, String condition, Integer humidityPercent,
                                 Integer windLevel, String error) {}
    public record ForecastResult(boolean supported, String cityCode, List<ForecastDay> days, String error) {}
    public record ForecastDay(String date, int minC, int maxC, String condition) {}
}
