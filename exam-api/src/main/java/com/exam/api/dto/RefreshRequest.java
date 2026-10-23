package com.exam.api.dto;

import jakarta.validation.constraints.NotBlank;

/** 刷新 token 入参 */
public record RefreshRequest(
        @NotBlank(message = "refreshToken 不能为空") String refreshToken) {
}
