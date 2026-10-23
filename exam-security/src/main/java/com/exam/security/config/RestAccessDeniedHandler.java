package com.exam.security.config;

import com.exam.common.constant.ErrorCode;
import com.exam.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/** 已登录但无权限时返回 403 + 统一响应体 */
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper json = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getWriter(), Result.fail(ErrorCode.FORBIDDEN, "没有权限执行该操作"));
    }
}
