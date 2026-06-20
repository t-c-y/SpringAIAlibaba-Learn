package com.example.springaialibaba.envcheck;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;

import java.time.Duration;

/**
 * DashScope HTTP 客户端配置。
 *
 * Spring AI Alibaba 1.0.0.2 的 DashScope 自动配置会使用 Spring 的 RestClient 发起 HTTP 请求。
 * 本配置通过 RestClientCustomizer 显式设置 OkHttp 的连接、读取和写入超时时间，
 * 避免模型响应较慢时仍使用较短的默认超时。
 */
@Configuration
public class DashScopeHttpClientConfiguration {

    @Bean
    public RestClientCustomizer dashScopeRestClientCustomizer(
            @Value("${spring.ai.dashscope.connect-timeout:10000}") long connectTimeout,
            @Value("${spring.ai.dashscope.read-timeout:120000}") long readTimeout,
            @Value("${spring.ai.dashscope.write-timeout:120000}") long writeTimeout) {
        return builder -> {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(Duration.ofMillis(connectTimeout))
                    .readTimeout(Duration.ofMillis(readTimeout))
                    .writeTimeout(Duration.ofMillis(writeTimeout))
                    .build();

            OkHttp3ClientHttpRequestFactory requestFactory = new OkHttp3ClientHttpRequestFactory(okHttpClient);
            builder.requestFactory(requestFactory);
        };
    }
}
