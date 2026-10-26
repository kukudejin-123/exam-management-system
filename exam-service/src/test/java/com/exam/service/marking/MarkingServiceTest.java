package com.exam.service.marking;

import com.exam.common.exception.BizException;
import com.exam.repository.entity.ExamAttempt;
import com.exam.repository.entity.Score;
import com.exam.repository.mapper.ExamAttemptMapper;
import com.exam.repository.mapper.ScoreMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 阅卷队列单元测试。
 *
 * <p>重点验证三条约束：匿名评阅、未交卷不能阅、打分幂等不累加。
 */
@ExtendWith(MockitoExtension.class)
class MarkingServiceTest {

    @Mock
    private ExamAttemptMapper attemptMapper;
    @Mock
    private ScoreMapper scoreMapper;

    private MarkingService markingService;

    @BeforeEach
    void setUp() {
        markingService = new MarkingService(attemptMapper, scoreMapper);
    }

    private ExamAttemptMapper.MarkingRow row(long attemptId, long studentId, String name,
                                             String answer, Integer gained) {
        return new ExamAttemptMapper.MarkingRow(attemptId, studentId, name, answer, gained);
    }

    private ExamAttempt submittedAttempt(long id, long examId, long studentId) {
        ExamAttempt a = new ExamAttempt();
        a.setId(id);
        a.setExamId(examId);
        a.setStudentId(studentId);
        a.setStatus(1);   // 已交卷
        return a;
    }

    // ---------- 队列 ----------

    @Test
    @DisplayName("默认匿名：不返回考生姓名")
    void queue_is_anonymous_by_default() {
        when(attemptMapper.selectMarkingRows(1L, 100L)).thenReturn(List.of(
                row(10L, 5L, "王小明", "栈是后进先出", null),
                row(11L, 6L, "李小红", "队列是先进先出", null)));

        List<MarkingItem> queue = markingService.queue(1L, 100L, false);

        assertThat(queue).hasSize(2);
        assertThat(queue).allMatch(i -> i.studentName() == null);
        // 但 studentId 保留，用于定位记录
        assertThat(queue).extracting(MarkingItem::studentId).containsExactly(5L, 6L);
    }

    @Test
    @DisplayName("实名复核：显式开启后返回姓名")
    void queue_can_reveal_names() {
        when(attemptMapper.selectMarkingRows(1L, 100L)).thenReturn(List.of(
                row(10L, 5L, "王小明", "栈是后进先出", null)));

        List<MarkingItem> queue = markingService.queue(1L, 100L, true);

        assertThat(queue.get(0).studentName()).isEqualTo("王小明");
    }

    @Test
    @DisplayName("未作答的条目不进队列")
    void queue_skips_blank_answers() {
        when(attemptMapper.selectMarkingRows(1L, 100L)).thenReturn(List.of(
                row(10L, 5L, "王小明", "栈是后进先出", null),
                row(11L, 6L, "李小红", "   ", null),
                row(12L, 7L, "赵小刚", null, null)));

        List<MarkingItem> queue = markingService.queue(1L, 100L, false);

        assertThat(queue).hasSize(1);
        assertThat(queue.get(0).attemptId()).isEqualTo(10L);
    }

    // ---------- 打分 ----------

    @Test
    @DisplayName("已交卷的作答可以打分，并刷新总分与成绩")
    void grade_updates_scores() {
        when(attemptMapper.selectById(10L)).thenReturn(submittedAttempt(10L, 1L, 5L));
        when(attemptMapper.upsertItemScore(10L, 100L, 8, 2L)).thenReturn(1);
        when(attemptMapper.sumAttemptScore(10L)).thenReturn(8);

        markingService.grade(10L, 100L, 8, 2L);

        verify(attemptMapper).updateAttemptScore(10L, 8);
        verify(scoreMapper).syncFromMarking(1L, 5L, 8, 2L);
    }

    @Test
    @DisplayName("未交卷不能阅卷")
    void grade_rejects_in_progress_attempt() {
        ExamAttempt a = submittedAttempt(10L, 1L, 5L);
        a.setStatus(0);   // 进行中
        when(attemptMapper.selectById(10L)).thenReturn(a);

        assertThatThrownBy(() -> markingService.grade(10L, 100L, 8, 2L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("尚未交卷");

        verify(attemptMapper, never()).upsertItemScore(anyLong(), anyLong(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("作答记录不存在时拒绝")
    void grade_rejects_missing_attempt() {
        when(attemptMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> markingService.grade(999L, 100L, 8, 2L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("作答记录不存在");
    }

    @Test
    @DisplayName("负分拒绝")
    void grade_rejects_negative_score() {
        assertThatThrownBy(() -> markingService.grade(10L, 100L, -1, 2L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能为负数");
    }

    @Test
    @DisplayName("打分写入 0 行时抛异常（该次尝试没有这道题）")
    void grade_rejects_no_such_item() {
        when(attemptMapper.selectById(10L)).thenReturn(submittedAttempt(10L, 1L, 5L));
        when(attemptMapper.upsertItemScore(10L, 100L, 8, 2L)).thenReturn(0);

        assertThatThrownBy(() -> markingService.grade(10L, 100L, 8, 2L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("没有这道题的作答记录");
    }

    @Test
    @DisplayName("重复打分覆盖旧分，不累加（幂等由 SQL 的 ON DUPLICATE KEY 保证）")
    void grade_is_idempotent() {
        when(attemptMapper.selectById(10L)).thenReturn(submittedAttempt(10L, 1L, 5L));
        when(attemptMapper.upsertItemScore(eq(10L), eq(100L), anyInt(), anyLong())).thenReturn(1);
        // 数据库侧的 ON DUPLICATE KEY 保证覆盖，这里模拟"总分始终等于最后一笔得分"
        when(attemptMapper.sumAttemptScore(10L)).thenReturn(8, 5);

        markingService.grade(10L, 100L, 8, 2L);
        verify(attemptMapper).updateAttemptScore(10L, 8);

        // 改判 5 分：应覆盖而不是 8+5=13
        markingService.grade(10L, 100L, 5, 2L);
        verify(attemptMapper).updateAttemptScore(10L, 5);
        verify(scoreMapper).syncFromMarking(1L, 5L, 5, 2L);
    }

    // ---------- 进度 ----------

    @Test
    @DisplayName("进度统计已阅与待阅")
    void progress_counts_graded_and_pending() {
        when(attemptMapper.selectMarkingRows(1L, 100L)).thenReturn(List.of(
                row(10L, 5L, "王小明", "答1", 8),
                row(11L, 6L, "李小红", "答2", null),
                row(12L, 7L, "赵小刚", "答3", 6),
                row(13L, 8L, "陈小虎", "答4", null)));

        MarkingProgress p = markingService.progress(1L, 100L);

        assertThat(p.total()).isEqualTo(4);
        assertThat(p.graded()).isEqualTo(2);
        assertThat(p.pending()).isEqualTo(2);
        assertThat(p.percent()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("没有待阅时进度为 0，不除零")
    void progress_empty_is_zero() {
        when(attemptMapper.selectMarkingRows(1L, 100L)).thenReturn(List.of());

        MarkingProgress p = markingService.progress(1L, 100L);

        assertThat(p.total()).isZero();
        assertThat(p.percent()).isZero();
    }
}
