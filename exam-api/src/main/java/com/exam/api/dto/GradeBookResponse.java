package com.exam.api.dto;

import com.exam.service.score.ScoreService;

import java.util.List;

/** 成绩册响应 */
public record GradeBookResponse(long examId, String examName, int total, int pass, int fail,
                                int absent, int ungraded, List<Row> rows) {

    public static GradeBookResponse from(ScoreService.GradeBook book) {
        List<Row> rows = book.rows().stream()
                .map(r -> new Row(r.scoreId(), r.studentId(), r.userNo(), r.name(),
                        r.rawScore(), r.finalScore(), r.status(), r.statusLabel()))
                .toList();
        return new GradeBookResponse(book.examId(), book.examName(), book.total(),
                book.pass(), book.fail(), book.absent(), book.ungraded(), rows);
    }

    public record Row(Long scoreId, Long studentId, String userNo, String name,
                      Integer rawScore, Integer finalScore, Integer status, String statusLabel) {
    }
}
