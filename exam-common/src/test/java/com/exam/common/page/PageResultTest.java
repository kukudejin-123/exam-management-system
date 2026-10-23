package com.exam.common.page;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("分页结果体")
class PageResultTest {

    @Test
    @DisplayName("总页数按总条数整除计算")
    void pages_rounds_up() {
        PageResult<String> r = PageResult.of(Arrays.asList("a", "b"), 25, 1, 10);
        assertThat(r.getPages()).isEqualTo(3);
        assertThat(r.getTotal()).isEqualTo(25);
    }

    @Test
    @DisplayName("刚好整除不多算一页")
    void pages_exact_division() {
        PageResult<String> r = PageResult.of(Collections.emptyList(), 20, 1, 10);
        assertThat(r.getPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("总条数为 0 时返回 0 页")
    void pages_zero_total() {
        PageResult<String> r = PageResult.of(Collections.emptyList(), 0, 1, 10);
        assertThat(r.getPages()).isZero();
    }

    @Test
    @DisplayName("每页条数为 0 且有数据时按 1 条算，避免除零")
    void pages_guard_zero_page_size_with_data() {
        PageResult<String> r = PageResult.of(Arrays.asList("a", "b"), 2, 1, 0);
        assertThat(r.getPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("空结果返回空列表而非 null")
    void empty_has_empty_records() {
        PageResult<Object> r = PageResult.empty(1, 10);
        assertThat(r.getRecords()).isEmpty();
        assertThat(r.getTotal()).isZero();
    }

    @Test
    @DisplayName("records 为 null 时归一化为空列表")
    void null_records_normalized() {
        PageResult<Object> r = PageResult.of(null, 0, 1, 10);
        assertThat(r.getRecords()).isEmpty();
    }
}
