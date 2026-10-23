package com.exam.api.config;

import com.exam.common.constant.ErrorCode;
import com.exam.common.exception.BizException;
import com.exam.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理。
 *
 * <p>原则：业务异常返回明确的错误码，参数校验错误列出具体字段，
 * 未预期异常记日志但不把堆栈暴露给前端。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException e) {
        return ResponseEntity.status(statusOf(e.getCode()))
                .body(Result.fail(e.getCode(), e.getMessage()));
    }

    /** @Valid 校验失败（@RequestBody） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleBodyInvalid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::format)
                .collect(Collectors.joining("；"));
        return ResponseEntity.badRequest().body(Result.fail(ErrorCode.PARAM_INVALID, msg));
    }

    /** 表单/查询参数绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::format)
                .collect(Collectors.joining("；"));
        return ResponseEntity.badRequest().body(Result.fail(ErrorCode.PARAM_INVALID, msg));
    }

    /** 方法级权限不足（@PreAuthorize 拒绝） */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.fail(ErrorCode.FORBIDDEN, "没有权限执行该操作"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpected(Exception e) {
        log.error("未预期异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ErrorCode.INTERNAL_ERROR, "系统繁忙，请稍后重试"));
    }

    private String format(FieldError fe) {
        return fe.getField() + " " + fe.getDefaultMessage();
    }

    /** 错误码映射到 HTTP 状态：2xxx → 401，3xxx → 404，其余 400 */
    private HttpStatus statusOf(int code) {
        if (code >= 2001 && code <= 2005) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code == ErrorCode.FORBIDDEN) {
            return HttpStatus.FORBIDDEN;
        }
        if (code == ErrorCode.NOT_FOUND) {
            return HttpStatus.NOT_FOUND;
        }
        if (code == ErrorCode.INTERNAL_ERROR) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
