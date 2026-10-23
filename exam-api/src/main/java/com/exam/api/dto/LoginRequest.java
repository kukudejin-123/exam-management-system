package com.exam.api.dto;

import jakarta.validation.constraints.NotBlank;

/** 登录入参 */
public record LoginRequest(
        @NotBlank(message = "学号/工号不能为空") String userNo,
        @NotBlank(message = "密码不能为空") String password) {
}
