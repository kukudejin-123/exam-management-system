package com.exam.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 修改成绩入参 */
public record ScoreUpdateRequest(
        @NotNull(message = "成绩不能为空")
        @Min(value = 0, message = "成绩不能为负数")
        Integer finalScore) {
}
