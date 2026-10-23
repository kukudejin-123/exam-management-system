package com.exam.service.auth;

/** 登录成功返回的令牌对。 */
public record LoginResult(String accessToken, long accessExpiresIn, String refreshToken, long userId, String role) {

    public static LoginResult of(String accessToken, long accessExpiresIn, String refreshToken,
                                 long userId, String role) {
        return new LoginResult(accessToken, accessExpiresIn, refreshToken, userId, role);
    }
}
