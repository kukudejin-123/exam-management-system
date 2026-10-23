package com.exam.api.dto;

import com.exam.repository.entity.Question;

/** 题目响应体 */
public record QuestionResponse(Long id, Long bankId, String type, String stem,
                               String options, String answer, Integer difficulty,
                               String chapter, String knowledge, Integer status) {

    public static QuestionResponse from(Question q) {
        return new QuestionResponse(q.getId(), q.getBankId(), q.getType(), q.getStem(),
                q.getOptions(), q.getAnswer(), q.getDifficulty(),
                q.getChapter(), q.getKnowledge(), q.getStatus());
    }
}
