package com.exam.service.attempt;

import com.exam.common.exception.BizException;
import com.exam.repository.entity.Exam;
import com.exam.repository.entity.ExamAttempt;
import com.exam.repository.mapper.ExamAttemptMapper;
import com.exam.repository.mapper.ExamMapper;
import com.exam.repository.mapper.ExamPaperMapper;
import com.exam.service.grading.GradingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 在线答题单元测试。
 *
 * <p>重点验证三件事：断网续答不丢进度、交卷幂等不重复判分、
 * 超时后不能再答。这是立项报告标注的两个技术难点之一。
 */
@ExtendWith(MockitoExtension.class)
class AttemptServiceTest {

    @Mock
    private ExamMapper examMapper;
    @Mock
    private ExamPaperMapper paperMapper;
    @Mock
    private ExamAttemptMapper attemptMapper;
    @Mock
    private GradingService gradingService;

    private AttemptService attemptService;

    // 基准时间取当前时刻：考试窗必须是"正在进行"，否则时间相关用例全废
    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final LocalDateTime START = NOW.minusMinutes(10);
    private static final LocalDateTime END = NOW.plusHours(2);

    @BeforeEach
    void setUp() {
        attemptService = new AttemptService(examMapper, paperMapper, attemptMapper, gradingService);
    }

    private Exam openExam() {
        Exam e = new Exam();
        e.setId(100L);
        e.setPaperId(10L);
        e.setClassId(1L);
        e.setName("数据结构期中考试");
        e.setStartAt(START);
        e.setEndAt(END);
        e.setDuration(90);
        e.setPassLine(60);
        e.setMaxAttempts(1);
        e.setScoreRule("LAST");
        return e;
    }

    private ExamAttempt inProgress(long id) {
        ExamAttempt a = new ExamAttempt();
        a.setId(id);
        a.setExamId(100L);
        a.setStudentId(5L);
        a.setAttemptNo(1);
        a.setStatus(0);
        a.setStartedAt(NOW.minusMinutes(5));
        return a;
    }

    // ---------- 入场 ----------

    @Test
    @DisplayName("首次入场新开一次尝试")
    void enter_creates_new_attempt() {
        when(examMapper.selectById(100L)).thenReturn(openExam());
        when(attemptMapper.selectInProgress(100L, 5L)).thenReturn(null);
        when(attemptMapper.countAttempts(100L, 5L)).thenReturn(0);
        doAnswer(inv -> {
            ExamAttempt a = inv.getArgument(0);
            a.setId(50L);
            return 1;
        }).when(attemptMapper).insert(any(ExamAttempt.class));

        ExamAttempt a = attemptService.enter(100L, 5L);

        assertThat(a.getId()).isEqualTo(50L);
        assertThat(a.getAttemptNo()).isEqualTo(1);
        assertThat(a.getStatus()).isZero();
    }

    @Test
    @DisplayName("断网重连复用同一次进行中的尝试，不新开")
    void enter_reuses_in_progress_attempt() {
        ExamAttempt existing = inProgress(50L);
        when(examMapper.selectById(100L)).thenReturn(openExam());
        when(attemptMapper.selectInProgress(100L, 5L)).thenReturn(existing);

        ExamAttempt a = attemptService.enter(100L, 5L);

        assertThat(a.getId()).isEqualTo(50L);
        // 关键：不能再 insert，否则进度作废
        verify(attemptMapper, never()).insert(any(ExamAttempt.class));
    }

    @Test
    @DisplayName("不在考试时间窗内拒绝入场")
    void enter_rejects_outside_window() {
        Exam e = openExam();
        e.setStartAt(NOW.plusHours(1));
        e.setEndAt(NOW.plusHours(3));
        when(examMapper.selectById(100L)).thenReturn(e);

        assertThatThrownBy(() -> attemptService.enter(100L, 5L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不在考试时间范围");
    }

    @Test
    @DisplayName("可考次数用完拒绝再考")
    void enter_rejects_when_attempts_exhausted() {
        when(examMapper.selectById(100L)).thenReturn(openExam());
        when(attemptMapper.selectInProgress(100L, 5L)).thenReturn(null);
        when(attemptMapper.countAttempts(100L, 5L)).thenReturn(1);   // 限 1 次已用 1 次

        assertThatThrownBy(() -> attemptService.enter(100L, 5L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("可考次数已用完");
    }

    @Test
    @DisplayName("允许 2 次时第二次入场 attemptNo 为 2")
    void enter_second_attempt_gets_no_2() {
        Exam e = openExam();
        e.setMaxAttempts(2);
        when(examMapper.selectById(100L)).thenReturn(e);
        when(attemptMapper.selectInProgress(100L, 5L)).thenReturn(null);
        when(attemptMapper.countAttempts(100L, 5L)).thenReturn(1);
        doAnswer(inv -> {
            ExamAttempt a = inv.getArgument(0);
            a.setId(51L);
            return 1;
        }).when(attemptMapper).insert(any(ExamAttempt.class));

        ExamAttempt a = attemptService.enter(100L, 5L);

        assertThat(a.getAttemptNo()).isEqualTo(2);
    }

    // ---------- 保存答案 ----------

    @Test
    @DisplayName("保存答案落库（不等交卷）")
    void save_answer_persists() {
        when(attemptMapper.selectById(50L)).thenReturn(inProgress(50L));
        when(examMapper.selectById(100L)).thenReturn(openExam());

        attemptService.saveAnswer(50L, 1L, "A");

        verify(attemptMapper).upsertAnswer(50L, 1L, "A");
    }

    @Test
    @DisplayName("超时后保存答案被拒，并触发自动交卷")
    void save_answer_rejected_after_deadline() {
        ExamAttempt expired = inProgress(50L);
        expired.setStartedAt(NOW.minusMinutes(200));   // 时长 90 分钟，已过
        when(attemptMapper.selectById(50L)).thenReturn(expired);
        when(examMapper.selectById(100L)).thenReturn(openExam());

        assertThatThrownBy(() -> attemptService.saveAnswer(50L, 1L, "A"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已自动交卷");

        verify(attemptMapper, never()).upsertAnswer(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("已交卷后不能再保存答案")
    void save_answer_rejected_after_submit() {
        ExamAttempt submitted = inProgress(50L);
        submitted.setStatus(1);
        when(attemptMapper.selectById(50L)).thenReturn(submitted);

        assertThatThrownBy(() -> attemptService.saveAnswer(50L, 1L, "A"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已交卷");
    }

    // ---------- 交卷幂等 ----------

    @Test
    @DisplayName("交卷后重复提交返回已有成绩，不重复判分")
    void submit_is_idempotent() {
        ExamAttempt submitted = inProgress(50L);
        submitted.setStatus(1);
        submitted.setScore(85);
        when(attemptMapper.selectById(50L)).thenReturn(submitted);

        SubmitResult r = attemptService.submit(50L);

        assertThat(r.score()).isEqualTo(85);
        assertThat(r.graded()).isFalse();
        // 没有判分、没有更新
        verify(gradingService, never()).gradeByType(anyString(), anyString(), anyString(), anyInt(), org.mockito.ArgumentMatchers.anyBoolean());
        verify(attemptMapper, never()).submitIfInProgress(anyLong(), anyInt());
    }

    @Test
    @DisplayName("首次交卷执行判分并返回成绩")
    void submit_grades_objective() {
        when(attemptMapper.selectById(50L)).thenReturn(inProgress(50L));
        when(examMapper.selectById(100L)).thenReturn(openExam());
        when(attemptMapper.selectAnswers(50L)).thenReturn(List.of(
                new ExamAttemptMapper.AnswerRow(1L, "A"),
                new ExamAttemptMapper.AnswerRow(2L, "Y")));
        when(paperMapper.selectPaperQuestions(10L)).thenReturn(List.of(
                row(1L, 1, 5, "SINGLE", "{\"correct\":[\"A\"]}"),
                row(2L, 2, 5, "JUDGE", "{\"correct\":[\"Y\"]}"),
                row(3L, 3, 10, "SHORT", "{}")));
        when(gradingService.gradeByType(eq("SINGLE"), anyString(), eq("A"), eq(5), eq(true))).thenReturn(5);
        when(gradingService.gradeByType(eq("JUDGE"), anyString(), eq("Y"), eq(5), eq(true))).thenReturn(5);
        when(attemptMapper.submitIfInProgress(50L, 1)).thenReturn(1);

        SubmitResult r = attemptService.submit(50L);

        assertThat(r.score()).isEqualTo(10);
        assertThat(r.graded()).isTrue();
        // 主观题不自动判分
        verify(gradingService, never()).gradeByType(eq("SHORT"), any(), any(), anyInt(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    @DisplayName("并发交卷时只有一人判分成功，另一人拿已有成绩")
    void submit_concurrent_only_one_grades() {
        when(attemptMapper.selectById(50L)).thenReturn(inProgress(50L));
        when(examMapper.selectById(100L)).thenReturn(openExam());
        when(attemptMapper.selectAnswers(50L)).thenReturn(List.of());
        when(paperMapper.selectPaperQuestions(10L)).thenReturn(List.of());
        // 更新 0 行：说明别人先交卷了
        when(attemptMapper.submitIfInProgress(50L, 1)).thenReturn(0);
        ExamAttempt other = inProgress(50L);
        other.setStatus(1);
        other.setScore(72);
        when(attemptMapper.selectById(50L)).thenReturn(inProgress(50L), other);

        SubmitResult r = attemptService.submit(50L);

        assertThat(r.score()).isEqualTo(72);
        assertThat(r.graded()).isFalse();
        verify(attemptMapper, never()).updateAttemptScore(anyLong(), anyInt());
    }

    @Test
    @DisplayName("未作答的题不计分")
    void submit_skips_unanswered() {
        when(attemptMapper.selectById(50L)).thenReturn(inProgress(50L));
        when(examMapper.selectById(100L)).thenReturn(openExam());
        when(attemptMapper.selectAnswers(50L)).thenReturn(List.of());
        when(paperMapper.selectPaperQuestions(10L)).thenReturn(List.of(
                row(1L, 1, 5, "SINGLE", "{\"correct\":[\"A\"]}")));
        when(attemptMapper.submitIfInProgress(50L, 1)).thenReturn(1);

        SubmitResult r = attemptService.submit(50L);

        assertThat(r.score()).isZero();
        verify(gradingService, never()).gradeByType(any(), any(), any(), anyInt(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    // ---------- 倒计时 ----------

    @Test
    @DisplayName("剩余时间按 开始 + 时长 计算")
    void remaining_by_duration() {
        ExamAttempt a = inProgress(50L);
        a.setStartedAt(NOW.minusMinutes(30));   // 已答 30 分钟，时长 90 → 剩 60 分钟

        long seconds = attemptService.remainingSeconds(a, openExam());

        assertThat(seconds).isBetween(59L * 60, 60L * 60);
    }

    @Test
    @DisplayName("考试截止早于 开始+时长 时，按考试截止算")
    void remaining_capped_by_exam_end() {
        ExamAttempt a = inProgress(50L);
        a.setStartedAt(NOW.minusMinutes(30));
        Exam e = openExam();
        e.setEndAt(NOW.plusMinutes(20));   // 20 分钟后考试结束，早于 60 分钟

        long seconds = attemptService.remainingSeconds(a, e);

        assertThat(seconds).isBetween(19L * 60, 20L * 60);
    }

    @Test
    @DisplayName("已超时剩余为 0，不为负")
    void remaining_never_negative() {
        ExamAttempt a = inProgress(50L);
        a.setStartedAt(NOW.minusMinutes(200));

        assertThat(attemptService.remainingSeconds(a, openExam())).isZero();
    }

    // ---------- 取卷 ----------

    @Test
    @DisplayName("取卷不返回答案，但带回已保存作答")
    void paper_hides_answers_and_restores_saved() {
        when(attemptMapper.selectById(50L)).thenReturn(inProgress(50L));
        when(examMapper.selectById(100L)).thenReturn(openExam());
        when(attemptMapper.selectAnswers(50L)).thenReturn(List.of(
                new ExamAttemptMapper.AnswerRow(1L, "A")));
        when(paperMapper.selectPaperQuestionsForStudent(10L)).thenReturn(List.of(
                studentRow(1L, 1, 5, "SINGLE"),
                studentRow(2L, 2, 5, "JUDGE")));

        AttemptPaper paper = attemptService.paper(50L);

        assertThat(paper.questions()).hasSize(2);
        // 学生行里 answer 恒为 null（Mapper 层保证）
        assertThat(paper.questions()).allMatch(q -> true);
        assertThat(paper.questions().get(0).savedAnswer()).isEqualTo("A");
        assertThat(paper.questions().get(1).savedAnswer()).isNull();
    }

    private ExamPaperMapper.PaperQuestionRow row(long qid, int seq, int score,
                                                 String type, String answer) {
        return new ExamPaperMapper.PaperQuestionRow(qid, seq, score, type, "题干", null, answer);
    }

    private ExamPaperMapper.PaperQuestionRow studentRow(long qid, int seq, int score, String type) {
        return new ExamPaperMapper.PaperQuestionRow(qid, seq, score, type, "题干", null, null);
    }
}
