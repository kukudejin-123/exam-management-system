package com.exam.service.exam;

import com.exam.common.constant.ErrorCode;
import com.exam.common.exception.BizException;
import com.exam.repository.entity.Exam;
import com.exam.repository.entity.ExamPaper;
import com.exam.repository.entity.SchoolClass;
import com.exam.repository.entity.Score;
import com.exam.repository.entity.User;
import com.exam.repository.mapper.ExamMapper;
import com.exam.repository.mapper.ExamPaperMapper;
import com.exam.repository.mapper.SchoolClassMapper;
import com.exam.repository.mapper.ScoreMapper;
import com.exam.repository.mapper.UserMapper;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 考试发布单元测试。
 *
 * <p>重点验证：未发布的试卷不能用于考试、时间窗与时长的一致性、
 * 及格线不超过总分、创建时为每名学生建缺考成绩行。
 */
@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 1, 9, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 11, 1, 11, 0);

    @Mock
    private ExamMapper examMapper;
    @Mock
    private ExamPaperMapper paperMapper;
    @Mock
    private SchoolClassMapper classMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ScoreMapper scoreMapper;

    private ExamService examService;

    @BeforeEach
    void setUp() {
        examService = new ExamService(examMapper, paperMapper, classMapper, userMapper, scoreMapper);
    }

    private ExamPaper publishedPaper() {
        ExamPaper p = new ExamPaper();
        p.setId(10L);
        p.setName("期中卷");
        p.setStatus(2);          // 已发布
        p.setTotalScore(100);
        p.setQuestionCount(20);
        return p;
    }

    private Exam validExam() {
        Exam e = new Exam();
        e.setPaperId(10L);
        e.setClassId(1L);
        e.setName("数据结构期中考试");
        e.setStartAt(START);
        e.setEndAt(END);
        e.setDuration(90);
        e.setPassLine(60);
        e.setMaxAttempts(1);
        e.setScoreRule("LAST");
        e.setPublishMode("IMMEDIATE");
        e.setRetakePolicy(0);
        e.setCreatedBy(2L);
        return e;
    }

    private void stubLookups() {
        when(paperMapper.selectById(10L)).thenReturn(publishedPaper());
        when(classMapper.selectById(1L)).thenReturn(schoolClass());
    }

    private SchoolClass schoolClass() {
        SchoolClass c = new SchoolClass();
        c.setId(1L);
        c.setName("计科2401");
        c.setSubject("数据结构");
        return c;
    }

    private User student(long id, String no) {
        User u = new User();
        u.setId(id);
        u.setUserNo(no);
        u.setRole("STUDENT");
        u.setStatus(1);
        u.setClassId(1L);
        return u;
    }

    private void stubStudents(int count) {
        List<User> students = new java.util.ArrayList<>();
        for (int i = 1; i <= count; i++) {
            students.add(student((long) i, "S2024" + String.format("%03d", i)));
        }
        when(userMapper.selectList(any())).thenReturn(students);
    }

    // ---------- 创建 ----------

    @Test
    @DisplayName("合法参数创建成功，并为每名学生建缺考成绩行")
    void create_ok_creates_absent_rows() {
        stubLookups();
        stubStudents(3);
        doAnswer(inv -> {
            Exam e = inv.getArgument(0);
            e.setId(100L);
            return 1;
        }).when(examMapper).insert(any(Exam.class));

        long id = examService.create(validExam());

        assertThat(id).isEqualTo(100L);
        ArgumentCaptor<Score> captor = ArgumentCaptor.forClass(Score.class);
        verify(scoreMapper, org.mockito.Mockito.times(3)).insert(captor.capture());
        assertThat(captor.getAllValues())
                .allMatch(s -> s.getStatus() == Score.Status.ABSENT.code())
                .allMatch(s -> s.getExamId().equals(100L))
                .allMatch(s -> s.getClassId().equals(1L));
    }

    @Test
    @DisplayName("试卷未发布时拒绝创建")
    void create_rejects_unpublished_paper() {
        ExamPaper draft = publishedPaper();
        draft.setStatus(1);   // 草稿
        when(paperMapper.selectById(10L)).thenReturn(draft);

        assertThatThrownBy(() -> examService.create(validExam()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("尚未发布");

        verify(examMapper, never()).insert(any(Exam.class));
    }

    @Test
    @DisplayName("试卷不存在时拒绝创建")
    void create_rejects_missing_paper() {
        when(paperMapper.selectById(10L)).thenReturn(null);

        assertThatThrownBy(() -> examService.create(validExam()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("试卷不存在");
    }

    @Test
    @DisplayName("班级不存在时拒绝创建")
    void create_rejects_missing_class() {
        when(paperMapper.selectById(10L)).thenReturn(publishedPaper());
        when(classMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> examService.create(validExam()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("班级不存在");
    }

    @Test
    @DisplayName("截止时间早于开始时间时拒绝")
    void create_rejects_reversed_window() {
        stubLookups();
        Exam e = validExam();
        e.setStartAt(END);
        e.setEndAt(START);

        assertThatThrownBy(() -> examService.create(e))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("截止时间必须晚于开始时间");
    }

    @Test
    @DisplayName("答题时长超过时间窗时拒绝（否则可跨截止时间继续答）")
    void create_rejects_duration_over_window() {
        stubLookups();
        Exam e = validExam();
        e.setDuration(130);   // 时间窗只有 120 分钟

        assertThatThrownBy(() -> examService.create(e))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("超过了考试时间窗");
    }

    @Test
    @DisplayName("时长正好等于时间窗时允许")
    void create_allows_duration_equal_window() {
        stubLookups();
        stubStudents(1);
        doAnswer(inv -> {
            Exam x = inv.getArgument(0);
            x.setId(100L);
            return 1;
        }).when(examMapper).insert(any(Exam.class));

        Exam e = validExam();
        e.setDuration(120);

        assertThat(examService.create(e)).isEqualTo(100L);
    }

    @Test
    @DisplayName("及格线超过试卷总分时拒绝")
    void create_rejects_pass_line_over_total() {
        stubLookups();
        Exam e = validExam();
        e.setPassLine(120);   // 总分 100

        assertThatThrownBy(() -> examService.create(e))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("超过了试卷总分");
    }

    @Test
    @DisplayName("成绩取值规则非法时拒绝")
    void create_rejects_bad_score_rule() {
        stubLookups();
        Exam e = validExam();
        e.setScoreRule("MEDIAN");

        assertThatThrownBy(() -> examService.create(e))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("HIGHEST / LAST / AVERAGE");
    }

    @Test
    @DisplayName("可考次数为 0 时拒绝")
    void create_rejects_zero_attempts() {
        stubLookups();
        Exam e = validExam();
        e.setMaxAttempts(0);

        assertThatThrownBy(() -> examService.create(e))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("至少为 1");
    }

    @Test
    @DisplayName("班级没有学生时也能创建，只是名单为空")
    void create_ok_with_no_students() {
        stubLookups();
        when(userMapper.selectList(any())).thenReturn(List.of());
        doAnswer(inv -> {
            Exam x = inv.getArgument(0);
            x.setId(100L);
            return 1;
        }).when(examMapper).insert(any(Exam.class));

        assertThat(examService.create(validExam())).isEqualTo(100L);
        verify(scoreMapper, never()).insert(any());
    }

    // ---------- 查询与名单 ----------

    @Test
    @DisplayName("查询不存在的考试抛 404")
    void get_missing_exam() {
        when(examMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> examService.getById(999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("考试不存在");
    }

    @Test
    @DisplayName("名单按缺考/非缺考分类")
    void roster_splits_absent_and_attended() {
        Exam exam = validExam();
        exam.setId(100L);
        when(examMapper.selectById(100L)).thenReturn(exam);

        Score absent = new Score();
        absent.setStudentId(1L);
        absent.setClassId(1L);
        absent.setExamId(100L);
        absent.setStatus(Score.Status.ABSENT.code());

        Score attended = new Score();
        attended.setStudentId(2L);
        attended.setClassId(1L);
        attended.setExamId(100L);
        attended.setStatus(Score.Status.NORMAL.code());
        attended.setRawScore(85);

        when(scoreMapper.selectList(any())).thenReturn(List.of(absent, attended));

        ExamService.ExamRoster roster = examService.roster(100L);

        assertThat(roster.expected()).isEqualTo(2);
        assertThat(roster.absent()).isEqualTo(1);
        assertThat(roster.attended()).isEqualTo(1);
    }

    // ---------- 时间窗判断 ----------

    @Test
    @DisplayName("时间窗内判定为开放")
    void is_open_within_window() {
        Exam e = validExam();
        assertThat(examService.isOpen(e, START.plusMinutes(30))).isTrue();
    }

    @Test
    @DisplayName("开始前与截止后都判定为关闭")
    void is_closed_outside_window() {
        Exam e = validExam();
        assertThat(examService.isOpen(e, START.minusMinutes(1))).isFalse();
        assertThat(examService.isOpen(e, END.plusMinutes(1))).isFalse();
    }
}
