package com.exam.api.controller;

import com.exam.api.dto.LoginRequest;
import com.exam.api.dto.RefreshRequest;
import com.exam.api.dto.RegisterRequest;
import com.exam.api.dto.TokenResponse;
import com.exam.common.result.Result;
import com.exam.security.jwt.AuthPrincipal;
import com.exam.security.jwt.JwtService;
import com.exam.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册 / 登录 / 刷新 / 登出。
 *
 * <p>对应实训内容 4 的"用户的注册及登录"。
 * 这些路径在 SecurityConfig 里已放行（除登出外），登出需要携带 token 才知道该拉黑谁。
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        long id = authService.register(request.userNo(), request.name(),
                request.password(), request.role(), request.classId());
        return Result.ok(id);
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(TokenResponse.from(authService.login(request.userNo(), request.password())));
    }

    @Operation(summary = "刷新 access token")
    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.ok(TokenResponse.from(authService.refresh(request.refreshToken())));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @AuthenticationPrincipal AuthPrincipal principal) {
        String token = authorization.substring("Bearer ".length()).trim();
        authService.logout(token);
        return Result.ok();
    }
}
