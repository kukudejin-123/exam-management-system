package com.exam.service.score;

import com.exam.common.exception.BizException;
import com.exam.repository.entity.Exam;
import com.exam.repository.entity.OperationLog;
import com.exam.repository.entity.Score;
import com.exam.repository.entity.User;
import com.exam.repository.mapper.ExamAttemptMapper;
import com.exam.repository.mapper.ExamMapper;
import com.exam.repository.mapper.OperationLogMapper;
import com.exam.repository.mapper.ScoreMapper;
import com.exam.repository.mapper.UserMapper;
import com.exam.service.grading.GradingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 成绩管理单元测试。
 *
 * <p>重点验证：三态区分、总评加权口径、修改必须留痕、学生只能看自己的。
 */
@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    @Mock
    private ScoreMapper scoreMapper;
    @Mock
    private ExamMapper examMapper;
    @Mock
    private ExamAttemptMapper attemptMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private OperationLogMapper logMapper;
    @Mock
    private GradingService gradingService;

    private ScoreService scoreService;

    @BeforeEach
    void setUp() {
        scoreService = new ScoreService(scoreMapper, examMapper, attemptMapper,
                userMapper, logMapper, gradingService, new ObjectMapper());
    }

    private Score scoreRow(long id, long studentId, Integer finalScore, int status) {
        Score s = new Score();
        s.setId(id);
        s.setStudentId(studentId);
        s.setClassId(1L);
        s.setExamId(100L);
        s.setRawScore(finalScore);
        s.setFinalScore(finalScore);
        s.setStatus(status);
        return s;
    }

    private User user(long id, String no, String name) {
        User u = new User();
        u.setId(id);
        u.setUserNo(no);
        u.setName(name);
        u.setRole("STUDENT");
        return u;
    }

    private Exam exam(long id, String name, int weight) {
        return exam(id, name, weight, 60);
    }

    private Exam exam(long id, String name, int weight, int passLine) {
        Exam e = new Exam();
        e.setId(id);
        e.setName(name);
        e.setClassId(1L);
        e.setWeight(weight);
        e.setPassLine(passLine);
        e.setStartAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        e.setDuration(90);
        e.setMaxAttempts(1);
        return e;
    }

    // ---------- 成绩册 ----------

    @Test
    @DisplayName("成绩册按三态分类统计")
    void grade_book_counts_three_states() {
        when(examMapper.selectById(100L)).thenReturn(exam(100L, "期中考试", 1));
        when(scoreMapper.selectList(any())).thenReturn(List.of(
                scoreRow(1L, 5L, 85, Score.Status.NORMAL.code()),
                scoreRow(2L, 6L, 55, Score.Status.NORMAL.code()),
                scoreRow(3L, 7L, null, Score.Status.ABSENT.code()),
                scoreRow(4L, 8L, null, Score.Status.UNGRADED.code()),
                scoreRow(5L, 9L, 0, Score.Status.ZERO.code())));
        when(userMapper.selectById(anyLong())).thenAnswer(inv -> {
            long id = inv.getArgument(0);
            return user(id, "S" + id, "学生" + id);
        });

        ScoreService.GradeBook book = scoreService.gradeBook(100L, "score", null);

        assertThat(book.total()).isEqualTo(5);
        assertThat(book.pass()).isEqualTo(1);
        assertThat(book.absent()).isEqualTo(1);
        assertThat(book.ungraded()).isEqualTo(1);
        // 55 分不及格 + 0 分 = 2 个不及格
        assertThat(book.fail()).isEqualTo(2);
    }

    @Test
    @DisplayName("成绩册默认按分数降序")
    void grade_book_sorted_by_score_desc() {
        when(examMapper.selectById(100L)).thenReturn(exam(100L, "期中考试", 1));
        when(scoreMapper.selectList(any())).thenReturn(List.of(
                scoreRow(1L, 5L, 60, Score.Status.NORMAL.code()),
                scoreRow(2L, 6L, 95, Score.Status.NORMAL.code()),
                scoreRow(3L, 7L, 80, Score.Status.NORMAL.code())));
        when(userMapper.selectById(anyLong())).thenAnswer(inv -> {
            long id = inv.getArgument(0);
            return user(id, "S" + id, "学生" + id);
        });

        ScoreService.GradeBook book = scoreService.gradeBook(100L, "score", null);

        assertThat(book.rows()).extracting(ScoreService.GradeBookRow::finalScore)
                .containsExactly(95, 80, 60);
    }

    @Test
    @DisplayName("按状态筛选只返回对应状态")
    void grade_book_filters_by_status() {
        when(examMapper.selectById(100L)).thenReturn(exam(100L, "期中考试", 1));
        when(scoreMapper.selectList(any())).thenReturn(List.of(
                scoreRow(1L, 5L, 85, Score.Status.NORMAL.code()),
                scoreRow(2L, 6L, null, Score.Status.ABSENT.code())));
        when(userMapper.selectById(anyLong())).thenAnswer(inv -> {
            long id = inv.getArgument(0);
            return user(id, "S" + id, "学生" + id);
        });

        ScoreService.GradeBook book = scoreService.gradeBook(100L, "score",
                String.valueOf(Score.Status.ABSENT.code()));

        assertThat(book.rows()).hasSize(1);
        assertThat(book.rows().get(0).statusLabel()).isEqualTo("缺考");
    }

    @Test
    @DisplayName("状态标签可读，不是数字")
    void grade_book_status_labels() {
        Score s = scoreRow(1L, 5L, 85, Score.Status.NORMAL.code());
        ScoreService.GradeBookRow row = new ScoreService.GradeBookRow(
                s.getId(), s.getStudentId(), "S1", "张三",
                s.getRawScore(), s.getFinalScore(), s.getPassed(), s.getStatus());
        assertThat(row.statusLabel()).isEqualTo("正常");

        Score absent = scoreRow(2L, 6L, null, Score.Status.ABSENT.code());
        ScoreService.GradeBookRow absentRow = new ScoreService.GradeBookRow(
                absent.getId(), absent.getStudentId(), "S2", "李四",
                absent.getRawScore(), absent.getFinalScore(), absent.getPassed(), absent.getStatus());
        assertThat(absentRow.statusLabel()).isEqualTo("缺考");
    }

    // ---------- 总评 ----------

    @Test
    @DisplayName("总评按权重加权平均")
    void final_scores_weighted_average() {
        when(examMapper.selectList(any())).thenReturn(List.of(
                exam(100L, "期中", 3),
                exam(101L, "期末", 7)));
        when(scoreMapper.selectList(any())).thenAnswer(inv -> {
            // 按 examId 区分：两次调用分别返回两场考试的成绩
            var wrapper = inv.getArgument(0);
            return List.of(scoreRow(1L, 5L, 80, Score.Status.NORMAL.code()));
        });
        when(userMapper.selectById(5L)).thenReturn(user(5L, "S5", "王小明"));

        List<ScoreService.FinalScoreRow> rows = scoreService.finalScores(1L);

        assertThat(rows).hasSize(1);
        // 两场都是 80 分，加权后仍是 80
        assertThat(rows.get(0).finalScore()).isEqualTo(80);
        assertThat(rows.get(0).details()).hasSize(2);
    }

    @Test
    @DisplayName("缺考的考试不计入总评分子与分母")
    void final_scores_excludes_absent() {
        when(examMapper.selectList(any())).thenReturn(List.of(
                exam(100L, "期中", 5),
                exam(101L, "期末", 5)));
        // 期中 80 分，期末缺考
        when(scoreMapper.selectList(any()))
                .thenReturn(List.of(scoreRow(1L, 5L, 80, Score.Status.NORMAL.code())))
                .thenReturn(List.of(scoreRow(2L, 5L, null, Score.Status.ABSENT.code())));
        when(userMapper.selectById(5L)).thenReturn(user(5L, "S5", "王小明"));

        List<ScoreService.FinalScoreRow> rows = scoreService.finalScores(1L);

        // 若把缺考按 0 分计入，总评会是 40；按口径应为 80
        assertThat(rows.get(0).finalScore()).isEqualTo(80);
    }

    @Test
    @DisplayName("没有设置权重的考试不参与总评")
    void final_scores_ignores_zero_weight() {
        when(examMapper.selectList(any())).thenReturn(List.of(exam(100L, "期中", 0)));

        assertThat(scoreService.finalScores(1L)).isEmpty();
    }

    @Test
    @DisplayName("总评按分数降序，可排名")
    void final_scores_sorted_desc() {
        when(examMapper.selectList(any())).thenReturn(List.of(exam(100L, "期中", 1)));
        when(scoreMapper.selectList(any())).thenReturn(List.of(
                scoreRow(1L, 5L, 70, Score.Status.NORMAL.code()),
                scoreRow(2L, 6L, 90, Score.Status.NORMAL.code())));
        when(userMapper.selectById(5L)).thenReturn(user(5L, "S5", "王"));
        when(userMapper.selectById(6L)).thenReturn(user(6L, "S6", "李"));

        List<ScoreService.FinalScoreRow> rows = scoreService.finalScores(1L);

        assertThat(rows).extracting(ScoreService.FinalScoreRow::finalScore)
                .containsExactly(90, 70);
    }

    // ---------- 修改留痕 ----------

    @Test
    @DisplayName("修改成绩必须写操作日志，且含改前改后")
    void update_score_writes_log() {
        Score s = scoreRow(1L, 5L, 60, Score.Status.NORMAL.code());
        when(scoreMapper.selectById(1L)).thenReturn(s);

        scoreService.updateScore(1L, 75, 2L);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(logMapper).insert(captor.capture());
        OperationLog log = captor.getValue();
        assertThat(log.getUserId()).isEqualTo(2L);
        assertThat(log.getAction()).isEqualTo("SCORE_UPDATE");
        assertThat(log.getBeforeVal()).contains("60");
        assertThat(log.getAfterVal()).contains("75");
    }

    @Test
    @DisplayName("成绩没变化时不写日志")
    void update_score_noop_when_unchanged() {
        when(scoreMapper.selectById(1L)).thenReturn(scoreRow(1L, 5L, 75, Score.Status.NORMAL.code()));

        scoreService.updateScore(1L, 75, 2L);

        verify(logMapper, never()).insert(any());
        verify(scoreMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("缺考成绩不能直接改，须先安排补考")
    void update_score_rejects_absent() {
        when(scoreMapper.selectById(1L)).thenReturn(
                scoreRow(1L, 5L, null, Score.Status.ABSENT.code()));

        assertThatThrownBy(() -> scoreService.updateScore(1L, 75, 2L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("补考");
    }

    @Test
    @DisplayName("日志写入失败时取消修改")
    void update_score_rolls_back_when_log_fails() {
        when(scoreMapper.selectById(1L)).thenReturn(scoreRow(1L, 5L, 60, Score.Status.NORMAL.code()));
        when(logMapper.insert(any())).thenThrow(new RuntimeException("日志表满了"));

        assertThatThrownBy(() -> scoreService.updateScore(1L, 75, 2L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("日志写入失败");

        verify(scoreMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("成绩不存在时拒绝")
    void update_score_rejects_missing() {
        when(scoreMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> scoreService.updateScore(999L, 75, 2L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("成绩不存在");
    }

    // ---------- 学生查分 ----------

    @Test
    @DisplayName("学生只能查自己的成绩")
    void my_scores_rejects_other_student() {
        assertThatThrownBy(() -> scoreService.myScores(5L, 6L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("只能查询自己的成绩");
    }

    @Test
    @DisplayName("查自己的成绩带上考试名称")
    void my_scores_returns_exam_names() {
        when(scoreMapper.selectList(any())).thenReturn(List.of(
                scoreRow(1L, 5L, 85, Score.Status.NORMAL.code())));
        when(examMapper.selectById(100L)).thenReturn(exam(100L, "期中考试", 1));

        List<ScoreService.MyScoreRow> rows = scoreService.myScores(5L, 5L);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).examName()).isEqualTo("期中考试");
        assertThat(rows.get(0).finalScore()).isEqualTo(85);
    }
}
