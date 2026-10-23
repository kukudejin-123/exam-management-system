package com.exam.common.result;

import com.exam.common.constant.ErrorCode;
import com.exam.common.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("统一响应体")
class ResultTest {

    @Test
    @DisplayName("成功响应的 code 为 0 且 isSuccess 为真")
    void ok_is_success() {
        Result<String> r = Result.ok("hello");
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getCode()).isZero();
        assertThat(r.getData()).isEqualTo("hello");
    }

    @Test
    @DisplayName("失败响应保留错误码与信息")
    void fail_keeps_code_and_message() {
        Result<Object> r = Result.fail(ErrorCode.NOT_FOUND, "班级不存在");
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getCode()).isEqualTo(3001);
        assertThat(r.getMessage()).isEqualTo("班级不存在");
    }

    @Test
    @DisplayName("业务异常携带错误码")
    void biz_exception_carries_code() {
        BizException e = BizException.notFound("题目");
        assertThat(e.getCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(e.getMessage()).isEqualTo("题目不存在");
    }

    @Test
    @DisplayName("相等性按 code/message/data 判断")
    void equals_by_value() {
        assertThat(Result.ok("a")).isEqualTo(Result.ok("a"));
        assertThat(Result.ok("a")).isNotEqualTo(Result.ok("b"));
    }
}
