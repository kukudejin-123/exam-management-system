package com.exam.service.grading;

import com.exam.common.constant.ErrorCode;
import com.exam.common.exception.BizException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 客观题自动判分。
 *
 * <p>对应"阅卷与判分"中客观题那一半，主观题走人工阅卷队列。
 * 判分规则可重入：同样的输入永远得到同样的分数，重复触发不产生重复成绩。
 */
@Service
public class GradingService {

    private final ObjectMapper json = new ObjectMapper();

    /**
     * 单选 / 判断：完全匹配才给分。
     *
     * @param answerJson 题目答案，如 {"correct":["A"]}
     * @param response   考生作答，如 "A"
     * @param fullScore  该题满分
     * @return 满分或 0
     */
    public int gradeSingleOrJudge(String answerJson, String response, int fullScore) {
        List<String> correct = parseCorrect(answerJson);
        if (correct.size() != 1) {
            throw new BizException(ErrorCode.PARAM_INVALID, "单选题/判断题的正确答案必须有且仅有一个");
        }
        return normalize(response).equalsIgnoreCase(correct.get(0)) ? fullScore : 0;
    }

    /**
     * 多选：默认"完全匹配才给分"；strict=false 时按正确选项占比给分，
     * 但错选一项即 0 分（避免蒙对一半拿一半分）。
     *
     * @param response 考生作答，如 "ABD"
     */
    public int gradeMulti(String answerJson, String response, int fullScore, boolean strict) {
        List<String> correct = parseCorrect(answerJson);
        if (correct.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "多选题的正确答案不能为空");
        }
        List<String> picked = parseOptions(response);

        boolean exact = picked.size() == correct.size() && correct.containsAll(picked);
        if (exact) {
            return fullScore;
        }
        if (strict) {
            return 0;
        }
        // 非严格：有错选直接 0；只漏选按正确比例给分
        boolean wrongPick = picked.stream().anyMatch(p -> !correct.contains(p));
        if (wrongPick) {
            return 0;
        }
        int right = (int) picked.stream().filter(correct::contains).count();
        return fullScore * right / correct.size();
    }

    /**
     * 填空：多个可接受答案，忽略大小写与首尾空白，命中任一即给分。
     *
     * @param answerJson {"accept":["p.next.next", "p->next->next"]}
     */
    public boolean gradeFill(String answerJson, String response) {
        Map<String, Object> map = parse(answerJson);
        Object accept = map.get("accept");
        if (!(accept instanceof List<?> list) || list.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "填空题必须提供至少一个可接受答案");
        }
        String normalized = normalize(response);
        for (Object o : list) {
            if (normalized.equalsIgnoreCase(normalize(String.valueOf(o)))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按题型分发判分。SHORT（简答）不在自动判分范围内，返回 null 表示需人工阅卷。
     */
    public Integer gradeByType(String type, String answerJson, String response,
                               int fullScore, boolean multiStrict) {
        return switch (type) {
            case "SINGLE", "JUDGE" -> gradeSingleOrJudge(answerJson, response, fullScore);
            case "MULTI" -> gradeMulti(answerJson, response, fullScore, multiStrict);
            case "FILL" -> gradeFill(answerJson, response) ? fullScore : 0;
            case "SHORT" -> null;
            default -> throw new BizException(ErrorCode.PARAM_INVALID, "未知题型: " + type);
        };
    }

    /** 是否及格。passLine 为 null 表示不设及格线，此时一律视为及格 */
    public boolean isPassed(int score, Integer passLine) {
        return passLine == null || score >= passLine;
    }

    /**
     * 多次尝试的成绩取值。
     *
     * @param rule HIGHEST 取最高 / LAST 取末次 / AVERAGE 取平均（向下取整）
     */
    public int aggregate(List<Integer> attemptScores, String rule) {
        if (attemptScores == null || attemptScores.isEmpty()) {
            return 0;
        }
        return switch (rule == null ? "LAST" : rule) {
            case "HIGHEST" -> Collections.max(attemptScores);
            case "AVERAGE" -> (int) Math.floor(attemptScores.stream()
                    .mapToInt(Integer::intValue).average().orElse(0));
            case "LAST" -> attemptScores.get(attemptScores.size() - 1);
            default -> throw new BizException(ErrorCode.SCORE_RULE_CONFLICT,
                    "未知成绩取值规则: " + rule);
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> parseCorrect(String answerJson) {
        Map<String, Object> map = parse(answerJson);
        Object correct = map.get("correct");
        if (!(correct instanceof List<?> list)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "答案格式应为 {\"correct\":[...]}");
        }
        return list.stream().map(String::valueOf).toList();
    }

    private List<String> parseOptions(String response) {
        if (response == null || response.isBlank()) {
            return List.of();
        }
        return List.of(normalize(response).split(""));
    }

    private Map<String, Object> parse(String raw) {
        try {
            return json.readValue(raw, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "答案 JSON 解析失败: " + raw);
        }
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().replace("，", ",");
    }
}
