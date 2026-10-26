package com.exam.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 保存作答入参 */
public record AnswerRequest(
        @NotNull(message = "questionId 不能为空") Long questionId,

        @NotBlank(message = "作答不能为空") String answer) {
}
