package com.exam.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 阅卷打分入参 */
public record MarkingRequest(
        @NotNull(message = "attemptId 不能为空") Long attemptId,

        @NotNull(message = "questionId 不能为空") Long questionId,

        @NotNull(message = "得分不能为空")
        @Min(value = 0, message = "得分不能为负数")
        Integer gained) {
}
