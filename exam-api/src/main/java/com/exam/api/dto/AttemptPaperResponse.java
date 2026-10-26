package com.exam.api.dto;

import com.exam.service.attempt.AttemptPaper;

import java.util.List;

/** 考生视角的卷面响应 */
public record AttemptPaperResponse(long attemptId, int attemptNo, String examName,
                                   Integer durationMinutes, long remainingSeconds,
                                   List<QuestionForStudent> questions) {

    public static AttemptPaperResponse from(AttemptPaper p) {
        List<QuestionForStudent> qs = p.questions().stream()
                .map(QuestionForStudent::from)
                .toList();
        return new AttemptPaperResponse(p.attemptId(), p.attemptNo(), p.examName(),
                p.durationMinutes(), p.remainingSeconds(), qs);
    }

    /** 考生视角的一道题：没有答案字段 */
    public record QuestionForStudent(long questionId, int seq, int score, String type,
                                     String stem, String options, String savedAnswer) {

        static QuestionForStudent from(com.exam.service.attempt.AttemptQuestion q) {
            return new QuestionForStudent(q.questionId(), q.seq(), q.score(), q.type(),
                    q.stem(), q.options(), q.savedAnswer());
        }
    }
}
