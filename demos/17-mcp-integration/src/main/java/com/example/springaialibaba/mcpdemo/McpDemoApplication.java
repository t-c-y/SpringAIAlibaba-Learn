package com.example.springaialibaba.mcpdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 阶段十七：MCP 教学版 Agent。
 *
 * MCP (Model Context Protocol) 的核心思想：把工具从“进程内注册”改为“通过标准协议动态发现和调用”。
 * 由于 MCP 官方 SDK 与 Spring AI Alibaba 的集成仍在演进，本课用一个 REST 风格的“模拟 MCP 服务器”
 * 代替 stdio 传输，把要点讲清楚：动态工具发现、按 tool_name + args 调用、结构化返回。
 *
 * 你需要同时启动两个进程：MockMcpServer（工具方） 和 McpAgent（消费方）。默认走 process 内启动，
 * 也可以拆成两个终端跑，参考 README。
 */
@SpringBootApplication
public class McpDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpDemoApplication.class, args);
    }
}
