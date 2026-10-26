package com.exam.api.dto;

import com.exam.repository.entity.Question;

/** 试卷题目响应 */
public record PaperQuestionResponse(Long questionId, int seq, int score, String type,
                                    String stem, String options, String answer) {

    public static PaperQuestionResponse from(com.exam.service.paper.PaperService.PaperQuestionView v) {
        return new PaperQuestionResponse(v.questionId(), v.seq(), v.score(),
                v.type(), v.stem(), v.options(), v.answer());
    }

    /** 预览用：还没有题号 */
    public static PaperQuestionResponse of(Question q, int score, int seq) {
        return new PaperQuestionResponse(q.getId(), seq, score, q.getType(),
                q.getStem(), q.getOptions(), q.getAnswer());
    }
}
