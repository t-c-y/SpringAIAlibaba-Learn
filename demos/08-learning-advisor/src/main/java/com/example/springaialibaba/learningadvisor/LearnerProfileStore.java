package com.example.springaialibaba.learningadvisor;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 长期记忆存储（教学版）。
 *
 * 与 ChatMemory 存储“对话历史”不同，这里存储的是“稳定的用户画像 + 学习计划”，
 * 例如姓名、目标岗位、已掌握技能、正在学习的课程等。这些数据不会每轮都塞进 Prompt，
 * 只在需要时以 profile 段落的形式拼进 System Prompt。
 *
 * 生产建议：用户画像应当存 DB，并做版本 + 审计。
 */
@Component
public class LearnerProfileStore {

    private final Map<String, LearnerProfile> profiles = new ConcurrentHashMap<>();

    public LearnerProfile getOrEmpty(String userId) {
        return profiles.getOrDefault(userId, LearnerProfile.empty(userId));
    }

    public void update(LearnerProfile p) {
        profiles.put(p.userId(), p);
    }

    public void clear(String userId) {
        profiles.remove(userId);
    }

    public record LearnerProfile(
            String userId,
            String name,
            String targetRole,        // 目标岗位，例如 “AI 应用架构师”
            List<String> mastered,    // 已掌握技能
            List<String> learning,    // 正在学习的方向
            String weeklyBudgetHours  // 每周可投入学习时间
    ) {
        public static LearnerProfile empty(String userId) {
            return new LearnerProfile(userId, null, null, List.of(), List.of(), null);
        }

        /** 把画像渲染成 System Prompt 中可读的文本。 */
        public String toSystemFragment() {
            if (name == null && targetRole == null && mastered.isEmpty() && learning.isEmpty()) {
                return "（暂无用户画像，请在回答中礼貌询问必要信息。）";
            }
            StringBuilder sb = new StringBuilder();
            if (name != null) sb.append("姓名：").append(name).append("\n");
            if (targetRole != null) sb.append("目标岗位：").append(targetRole).append("\n");
            if (!mastered.isEmpty()) sb.append("已掌握：").append(String.join("、", mastered)).append("\n");
            if (!learning.isEmpty()) sb.append("正在学习：").append(String.join("、", learning)).append("\n");
            if (weeklyBudgetHours != null) sb.append("每周学习时间：").append(weeklyBudgetHours).append("\n");
            return sb.toString();
        }
    }
}
