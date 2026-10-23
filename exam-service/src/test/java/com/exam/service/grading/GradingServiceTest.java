package com.exam.service.grading;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 判分规则单元测试。
 *
 * <p>判分是考试系统里最不能出错的一段——错一道选择题影响一个学生的成绩。
 * 这里用 Mockito 风格的纯单测（不连库、不起 Spring），把边界全覆盖。
 */
class GradingServiceTest {

    private final GradingService grading = new GradingService();

    @Nested
    @DisplayName("单选与判断")
    class SingleAndJudge {

        @Test
        @DisplayName("答对给满分")
        void correct_gives_full() {
            assertThat(grading.gradeSingleOrJudge("{\"correct\":[\"A\"]}", "A", 5)).isEqualTo(5);
        }

        @Test
        @DisplayName("答错给 0 分")
        void wrong_gives_zero() {
            assertThat(grading.gradeSingleOrJudge("{\"correct\":[\"A\"]}", "B", 5)).isZero();
        }

        @Test
        @DisplayName("大小写不敏感，答案两侧空白被忽略")
        void case_and_space_insensitive() {
            assertThat(grading.gradeSingleOrJudge("{\"correct\":[\"a\"]}", " A ", 5)).isEqualTo(5);
        }

        @Test
        @DisplayName("未作答给 0 分")
        void blank_response_zero() {
            assertThat(grading.gradeSingleOrJudge("{\"correct\":[\"A\"]}", "", 5)).isZero();
        }

        @Test
        @DisplayName("正确答案不是唯一时拒绝判分，避免误判")
        void rejects_non_unique_correct() {
            assertThatThrownBy(() ->
                    grading.gradeSingleOrJudge("{\"correct\":[\"A\",\"B\"]}", "A", 5))
                    .isInstanceOf(com.exam.common.exception.BizException.class)
                    .hasMessageContaining("有且仅有一个");
        }
    }

    @Nested
    @DisplayName("多选")
    class Multi {

        private static final String ANSWER = "{\"correct\":[\"A\",\"B\",\"D\"]}";

        @Test
        @DisplayName("严格模式下完全匹配才给分")
        void strict_requires_exact() {
            assertThat(grading.gradeMulti(ANSWER, "ABD", 6, true)).isEqualTo(6);
            assertThat(grading.gradeMulti(ANSWER, "AB", 6, true)).isZero();
            assertThat(grading.gradeMulti(ANSWER, "ABDE", 6, true)).isZero();
        }

        @Test
        @DisplayName("非严格模式漏选按比例给分")
        void lenient_partial_credit() {
            // 3 题中答对 2 题 → 6 * 2/3 = 4
            assertThat(grading.gradeMulti(ANSWER, "AB", 6, false)).isEqualTo(4);
        }

        @Test
        @DisplayName("非严格模式错选直接 0 分，防止蒙对一半拿一半分")
        void lenient_wrong_pick_zero() {
            assertThat(grading.gradeMulti(ANSWER, "ABC", 6, false)).isZero();
        }

        @Test
        @DisplayName("选项顺序不影响判分")
        void order_insensitive() {
            assertThat(grading.gradeMulti(ANSWER, "DBA", 6, true)).isEqualTo(6);
        }

        @Test
        @DisplayName("未作答给 0 分")
        void blank_zero() {
            assertThat(grading.gradeMulti(ANSWER, "", 6, false)).isZero();
        }
    }

    @Nested
    @DisplayName("填空")
    class Fill {

        @Test
        @DisplayName("命中任一可接受答案即给分")
        void any_accept_matches() {
            String answer = "{\"accept\":[\"p.next.next\", \"p->next->next\"]}";
            assertThat(grading.gradeFill(answer, "p.next.next")).isTrue();
            assertThat(grading.gradeFill(answer, "p->next->next")).isTrue();
        }

        @Test
        @DisplayName("忽略大小写与首尾空白")
        void normalized() {
            assertThat(grading.gradeFill("{\"accept\":[\"True\"]}", " true ")).isTrue();
        }

        @Test
        @DisplayName("未命中不给分")
        void no_match() {
            assertThat(grading.gradeFill("{\"accept\":[\"a\"]}", "b")).isFalse();
        }

        @Test
        @DisplayName("没有可接受答案时拒绝判分")
        void empty_accept_rejected() {
            assertThatThrownBy(() -> grading.gradeFill("{\"accept\":[]}", "x"))
                    .isInstanceOf(com.exam.common.exception.BizException.class);
        }
    }

    @Nested
    @DisplayName("按题型分发")
    class ByType {

        @Test
        @DisplayName("客观题直接给分")
        void objective_graded() {
            assertThat(grading.gradeByType("JUDGE", "{\"correct\":[\"Y\"]}", "Y", 2, true))
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("简答题返回 null，交给人工阅卷")
        void short_returns_null() {
            assertThat(grading.gradeByType("SHORT", "{}", "随便写", 10, true)).isNull();
        }

        @Test
        @DisplayName("未知题型抛异常而不是静默给 0 分")
        void unknown_type_rejected() {
            assertThatThrownBy(() -> grading.gradeByType("ESSAY", "{}", "x", 10, true))
                    .isInstanceOf(com.exam.common.exception.BizException.class)
                    .hasMessageContaining("未知题型");
        }
    }

    @Nested
    @DisplayName("及格判定与多次尝试汇总")
    class Aggregate {

        @Test
        @DisplayName("未设备格线时一律及格")
        void no_pass_line_always_passed() {
            assertThat(grading.isPassed(0, null)).isTrue();
        }

        @Test
        @DisplayName("达到及格线才算及格")
        void pass_line_boundary() {
            assertThat(grading.isPassed(60, 60)).isTrue();
            assertThat(grading.isPassed(59, 60)).isFalse();
        }

        @Test
        @DisplayName("HIGHEST 取最高分")
        void highest() {
            assertThat(grading.aggregate(List.of(70, 85, 60), "HIGHEST")).isEqualTo(85);
        }

        @Test
        @DisplayName("LAST 取末次")
        void last() {
            assertThat(grading.aggregate(List.of(70, 85, 60), "LAST")).isEqualTo(60);
        }

        @Test
        @DisplayName("AVERAGE 向下取整")
        void average_floors() {
            // (70 + 85 + 60) / 3 = 71.67 → 71
            assertThat(grading.aggregate(List.of(70, 85, 60), "AVERAGE")).isEqualTo(71);
        }

        @Test
        @DisplayName("空列表汇总为 0")
        void empty_aggregates_zero() {
            assertThat(grading.aggregate(List.of(), "HIGHEST")).isZero();
        }

        @Test
        @DisplayName("未知取值规则抛异常")
        void unknown_rule_rejected() {
            assertThatThrownBy(() -> grading.aggregate(List.of(1), "MEDIAN"))
                    .isInstanceOf(com.exam.common.exception.BizException.class);
        }
    }
}
