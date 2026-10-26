package com.exam.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** 创建考试入参 */
public record ExamRequest(
        @NotNull(message = "必须指定试卷") Long paperId,

        @NotNull(message = "必须指定班级") Long classId,

        @NotBlank(message = "考试名称不能为空")
        @Size(max = 128, message = "考试名称最长 128 个字符")
        String name,

        @NotNull(message = "必须指定开始时间") LocalDateTime startAt,

        @NotNull(message = "必须指定截止时间") LocalDateTime endAt,

        @NotNull(message = "必须指定答题时长")
        @Min(value = 1, message = "答题时长必须大于 0")
        Integer duration,

        @Min(value = 0, message = "及格线不能为负") Integer passLine,

        @Min(value = 1, message = "每人可考次数至少为 1") Integer maxAttempts,

        @Pattern(regexp = "^(HIGHEST|LAST|AVERAGE)$",
                message = "成绩取值规则必须是 HIGHEST / LAST / AVERAGE 之一")
        String scoreRule,

        @Pattern(regexp = "^(IMMEDIATE|DELAYED|MANUAL)$",
                message = "成绩发布模式必须是 IMMEDIATE / DELAYED / MANUAL 之一")
        String publishMode,

        @Min(value = 0, message = "补考策略只能是 0 或 1")
        @jakarta.validation.constraints.Max(value = 1, message = "补考策略只能是 0 或 1")
        Integer retakePolicy,

        @NotNull(message = "必须指定创建人") Long createdBy) {
}
