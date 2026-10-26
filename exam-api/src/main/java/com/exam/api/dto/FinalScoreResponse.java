package com.exam.api.dto;

import com.exam.service.score.ScoreService;

import java.util.List;

/** 总评响应 */
public record FinalScoreResponse(long studentId, String userNo, String name,
                                 int finalScore, boolean passed, List<ExamScore> details) {

    public static FinalScoreResponse from(ScoreService.FinalScoreRow row) {
        List<ExamScore> details = row.details().stream()
                .map(d -> new ExamScore(d.examId(), d.examName(), d.score(), d.weight()))
                .toList();
        return new FinalScoreResponse(row.studentId(), row.userNo(), row.name(),
                row.finalScore(), row.passed(), details);
    }

    public record ExamScore(long examId, String examName, int score, int weight) {
    }
}
