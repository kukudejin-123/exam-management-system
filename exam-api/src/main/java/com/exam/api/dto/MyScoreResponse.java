package com.exam.api.dto;

import com.exam.service.score.ScoreService;

/** 学生视角的成绩响应 */
public record MyScoreResponse(long examId, String examName, String startAt,
                              Integer finalScore, Integer passed, String statusLabel) {

    public static MyScoreResponse from(ScoreService.MyScoreRow row) {
        return new MyScoreResponse(row.examId(), row.examName(),
                row.startAt() == null ? null : row.startAt().toString(),
                row.finalScore(), row.passed(), row.statusLabel());
    }
}
