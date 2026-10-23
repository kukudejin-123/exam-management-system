package com.exam.api.dto;

import com.exam.service.auth.LoginResult;

/** 登录/刷新返回的令牌对 */
public record TokenResponse(String accessToken, long expiresIn, String refreshToken,
                            long userId, String role) {

    public static TokenResponse from(LoginResult r) {
        return new TokenResponse(r.accessToken(), r.accessExpiresIn(),
                r.refreshToken(), r.userId(), r.role());
    }
}
