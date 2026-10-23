package com.exam.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 题目创建/更新入参。
 *
 * <p>type 用正则枚举，与 service 层的校验互为兜底——
 * 入口层给友好提示，业务层保证规则不被绕过。
 */
public record QuestionRequest(
        @NotNull(message = "必须指定所属题库") Long bankId,

        @NotBlank(message = "题型不能为空")
        @Pattern(regexp = "^(SINGLE|MULTI|JUDGE|FILL|SHORT)$",
                message = "题型必须是 SINGLE/MULTI/JUDGE/FILL/SHORT 之一")
        String type,

        @NotBlank(message = "题干不能为空") String stem,

        String options,

        @NotBlank(message = "答案不能为空") String answer,

        @NotNull(message = "难度不能为空")
        @Min(value = 1, message = "难度最小为 1") @Max(value = 3, message = "难度最大为 3")
        Integer difficulty,

        @Size(max = 64, message = "章节名最长 64 个字符") String chapter,

        @Size(max = 128, message = "知识点最长 128 个字符") String knowledge,

        Integer status) {
}
