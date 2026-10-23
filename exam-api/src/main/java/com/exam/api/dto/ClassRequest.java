package com.exam.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 班级创建/更新入参 */
public record ClassRequest(
        @NotBlank(message = "班级名称不能为空")
        @Size(max = 128, message = "班级名称最长 128 个字符")
        String name,

        @NotBlank(message = "科目不能为空")
        @Size(max = 64, message = "科目最长 64 个字符")
        String subject,

        Long teacherId) {
}
