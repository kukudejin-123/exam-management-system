package com.exam.service.attempt;

/**
 * 考生视角的一道题。
 *
 * <p>**没有 answer 字段**——这不是遗漏，是安全要求。
 * 答案只存在于判分路径，不给考生。
 */
public record AttemptQuestion(long questionId, int seq, int score, String type,
                              String stem, String options, String savedAnswer) {
}
