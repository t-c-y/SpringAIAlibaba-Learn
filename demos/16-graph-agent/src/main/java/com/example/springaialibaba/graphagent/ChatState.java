package com.example.springaialibaba.graphagent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Graph 会话状态。节点通过读写这个对象来共享数据。
 */
public class ChatState {

    /** 当前节点。 */
    public String node;

    /** 用户 id 与本轮 user 输入。 */
    public String userId;
    public String userInput;

    /** LLM 判定的意图：ORDER_QUERY / POLICY_QA / REFUND / SMALL_TALK / UNKNOWN。 */
    public String intent;

    /** 每个节点产生的输出，最后按顺序合成响应。 */
    public final List<String> answerParts = new ArrayList<>();

    /** 结构化的中间变量池，节点之间共享。 */
    public final Map<String, Object> vars = new LinkedHashMap<>();

    /** 是否已终止。 */
    public boolean finished;

    public void append(String s) { if (s != null && !s.isBlank()) answerParts.add(s); }
    public String finalAnswer() { return String.join("\n\n", answerParts); }
}
