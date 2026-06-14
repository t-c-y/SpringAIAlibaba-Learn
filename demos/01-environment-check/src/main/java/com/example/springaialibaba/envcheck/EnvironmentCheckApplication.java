package com.example.springaialibaba.envcheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 阶段一 Demo 的 Spring Boot 启动类。
 *
 * 学习重点：
 * 1. Spring AI Alibaba 应用本质上仍然是一个标准 Spring Boot 应用。
 * 2. 只要引入了 Spring AI Alibaba Starter，并在 application.yml 中配置好模型参数，
 *    Spring Boot 启动时就会自动完成相关 Bean 的装配。
 * 3. 后续 Controller 中使用的 ChatClient.Builder，就是由 Spring AI / Spring AI Alibaba
 *    根据依赖和配置自动创建并注入到 Spring 容器中的。
 */
@SpringBootApplication
public class EnvironmentCheckApplication {

    /**
     * Java 应用入口。
     *
     * SpringApplication.run(...) 会完成以下事情：
     * 1. 启动 Spring 容器。
     * 2. 扫描当前包及子包下的 @Component、@RestController 等 Spring Bean。
     * 3. 读取 application.yml 配置文件。
     * 4. 根据 pom.xml 中引入的 Starter 执行自动配置。
     * 5. 启动内嵌 Web 服务器，默认是 Tomcat。
     */
    public static void main(String[] args) {
        SpringApplication.run(EnvironmentCheckApplication.class, args);
    }
}
