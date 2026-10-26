package com.exam.api.dto;

import com.exam.service.attempt.SubmitResult;

/** 交卷结果响应 */
public record SubmitResponse(Integer score, Integer status, boolean graded) {

    public static SubmitResponse from(SubmitResult r) {
        return new SubmitResponse(r.score(), r.status(), r.graded());
    }
}
