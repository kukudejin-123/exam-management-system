package com.exam.service.attempt;

/**
 * 交卷结果。
 *
 * @param graded 本次调用是否执行了判分。false 表示这是重复提交，
 *                返回的是此前已算出的成绩
 */
public record SubmitResult(Integer score, Integer status, boolean graded) {
}
