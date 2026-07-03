package com.example.springaialibaba.graphagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 阶段十六：Graph 客服 Agent。
 *
 * 说明：Spring AI Alibaba 1.0.0.2 里的 Graph 组件仍在快速演进；本课先教你 Graph 的核心思维
 * ——用确定性节点 + 显式状态迁移，代替“让 LLM 自由发挥”。等 API 稳定后你可以把节点体
 * 替换成官方 Graph SDK，不影响业务代码结构。
 */
@SpringBootApplication
public class GraphAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(GraphAgentApplication.class, args);
    }
}
