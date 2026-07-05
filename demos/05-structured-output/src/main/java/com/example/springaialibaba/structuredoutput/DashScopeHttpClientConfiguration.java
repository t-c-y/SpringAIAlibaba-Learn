package com.example.springaialibaba.structuredoutput;

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
 * Spring AI Alibaba 1.0.0.2 的 DashScope 自动配置会使用 Spring 的 RestClient 发起 HTTP 请求，
 * 但它并不会把 spring.ai.dashscope.read-timeout 等配置自动应用到底层 HTTP 客户端上。
 * 当 OkHttp 在 classpath 上时，Spring Boot 会默认用 OkHttp 作为 RestClient 的实现，
 * 而 OkHttp 的默认读超时只有 10 秒——对于 qwen3.7-max 这类带推理过程的模型来说远远不够，
 * 会出现 java.net.SocketTimeoutException: timeout。
 *
 * 本配置通过 RestClientCustomizer 显式设置 OkHttp 的连接、读取和写入超时时间，
 * 读取 spring.ai.dashscope.*-timeout 的配置值，避免大模型响应慢时被 10 秒默认超时截断。
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
