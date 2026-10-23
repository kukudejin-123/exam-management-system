package com.exam.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册入参。
 *
 * <p>学号格式限制在字母数字，长度 4~32；密码 6~64 位。
 * 校验规则写在入口层，业务层不再重复判断格式。
 */
public record RegisterRequest(
        @NotBlank(message = "学号/工号不能为空")
        @Pattern(regexp = "^[A-Za-z0-9]{4,32}$", message = "学号/工号只能包含字母数字，长度 4-32")
        String userNo,

        @NotBlank(message = "姓名不能为空")
        @Size(max = 64, message = "姓名最长 64 个字符")
        String name,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度须在 6-64 之间")
        String password,

        @NotBlank(message = "角色不能为空")
        @Pattern(regexp = "^(ADMIN|TEACHER|MARKER|STUDENT)$", message = "角色必须是 ADMIN/TEACHER/MARKER/STUDENT 之一")
        String role,

        Long classId) {
}
