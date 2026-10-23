package com.exam.common.page;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("分页入参归一化")
class PageQueryTest {

    @Test
    @DisplayName("默认第 1 页每页 10 条")
    void defaults() {
        PageQuery q = new PageQuery();
        assertThat(q.getPageNo()).isEqualTo(1);
        assertThat(q.getPageSize()).isEqualTo(10);
        assertThat(q.offset()).isZero();
    }

    @Test
    @DisplayName("第 3 页每页 20 条的偏移量为 40")
    void offset_for_page3_size20() {
        PageQuery q = new PageQuery();
        q.setPageNo(3);
        q.setPageSize(20);
        assertThat(q.offset()).isEqualTo(40);
    }

    @Test
    @DisplayName("每页条数超过上限时收敛到 100")
    void page_size_clamped_to_max() {
        PageQuery q = new PageQuery();
        q.setPageSize(500);
        assertThat(q.effectivePageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("非法页码按第 1 页处理，偏移量为 0")
    void invalid_page_no_treated_as_first() {
        PageQuery q = new PageQuery();
        q.setPageNo(0);
        assertThat(q.offset()).isZero();
    }
}
