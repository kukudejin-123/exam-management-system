package com.exam.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("脱敏工具")
class MaskUtilTest {

    @Test
    @DisplayName("三字姓名保留首尾，中间打星")
    void name_three_chars() {
        assertThat(MaskUtil.name("张小明")).isEqualTo("张*明");
    }

    @Test
    @DisplayName("两字姓名只保留姓")
    void name_two_chars() {
        assertThat(MaskUtil.name("李雷")).isEqualTo("李*");
    }

    @Test
    @DisplayName("单字姓名整串打星")
    void name_single_char() {
        assertThat(MaskUtil.name("王")).isEqualTo("*");
    }

    @Test
    @DisplayName("空值原样返回")
    void name_null_safe() {
        assertThat(MaskUtil.name(null)).isNull();
        assertThat(MaskUtil.name("")).isEmpty();
    }

    @Test
    @DisplayName("学号保留前 3 后 2")
    void user_no_masked() {
        assertThat(MaskUtil.userNo("2023010001")).isEqualTo("202*****01");
    }

    @Test
    @DisplayName("学号过短不处理")
    void user_no_short_untouched() {
        assertThat(MaskUtil.userNo("1234")).isEqualTo("1234");
    }
}
