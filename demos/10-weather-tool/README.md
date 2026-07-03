# 10 天气查询工具 Demo (外部 API 风格的 Tool)

阶段十目标：把 Tool Calling 从"数学函数"升级到"外部 API 风格"，学到工具返回结构化数据后如何被模型解读。

## 一、教学取舍

本课工具**不调用真实气象 API**，而是用 `WeatherTools` 里的静态 `Map` 提供 mock 数据。原因：

- 学习目标是"Tool Calling 的信息流"，不是"抢到 HeWeather 密钥"。
- 换成真实 API 时只需替换方法体，方法签名与 `@Tool` 描述不变。

支持的城市：`beijing / shanghai / hangzhou / shenzhen`。

## 二、两个工具方法

| 工具 | 场景 | 返回 |
| --- | --- | --- |
| `currentWeather(city)` | 现在的天气 | `CurrentWeather` record |
| `forecast(city)` | 未来 3 天 | `ForecastResult` record |

两处设计要点：

1. 未命中城市不抛异常，而是返回 `supported=false + error 字段`，让模型能生成友好话术。
2. 返回结构用 `record`，字段命名即 Schema 字段，方便模型直接引用。

## 三、运行

```bash
cd demos/10-weather-tool
export DASHSCOPE_API_KEY=你的 API Key
mvn spring-boot:run

# 支持的城市
curl -X POST 'http://localhost:8089/weather/ask' \
  -H 'Content-Type: application/json' \
  -d '{"question":"明后天要不要去杭州？"}'

# 未支持的城市
curl -X POST 'http://localhost:8089/weather/ask' \
  -H 'Content-Type: application/json' \
  -d '{"question":"今天成都天气如何？"}'
```

## 四、学习检查点

- [ ] 能说清楚为什么工具优先返回"结构化 + error 字段"，而不是直接抛异常。
- [ ] 明白"System Prompt 指令 + 工具描述"共同决定了模型的调用行为。
- [ ] 能列出把 Mock 升级为真实 API 时需要注意的点：超时、重试、鉴权、限流、错误码到用户话术的映射。

## 五、生产扩展方向

- 用 `RestClient` 调真实气象 API；把 API Key 注入到工具类里，而不是拼进 Prompt。
- 增加缓存（同城市 5 分钟内复用）。
- 在工具内部做参数校验（城市白名单、日期范围）。
- 对模型可见的错误信息与内部日志分离，避免把内部 stack trace 交给模型。
