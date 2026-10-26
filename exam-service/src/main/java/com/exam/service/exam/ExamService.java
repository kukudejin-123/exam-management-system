package com.exam.service.exam;

import com.exam.common.constant.ErrorCode;
import com.exam.common.exception.BizException;
import com.exam.repository.entity.Exam;
import com.exam.repository.entity.ExamPaper;
import com.exam.repository.entity.Score;
import com.exam.repository.entity.SchoolClass;
import com.exam.repository.entity.User;
import com.exam.repository.mapper.ExamMapper;
import com.exam.repository.mapper.ExamPaperMapper;
import com.exam.repository.mapper.SchoolClassMapper;
import com.exam.repository.mapper.ScoreMapper;
import com.exam.repository.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试发布与考场管理。
 *
 * <p>对应 F-05-1 考试创建与参数配置、F-05-2 考试入口、F-05-3 应考/实考/缺考名单。
 *
 * <p>**关键设计**：创建考试时为班级每名学生建一条成绩行（状态=缺考）。
 * 这样"缺考也占一行"，成绩册从发布那一刻就是完整的，
 * 不需要等考完再补数据；应考/实考/缺考三种状态也自然区分开。
 */
@Service
public class ExamService {

    private final ExamMapper examMapper;
    private final ExamPaperMapper paperMapper;
    private final SchoolClassMapper classMapper;
    private final UserMapper userMapper;
    private final ScoreMapper scoreMapper;

    public ExamService(ExamMapper examMapper,
                       ExamPaperMapper paperMapper,
                       SchoolClassMapper classMapper,
                       UserMapper userMapper,
                       ScoreMapper scoreMapper) {
        this.examMapper = examMapper;
        this.paperMapper = paperMapper;
        this.classMapper = classMapper;
        this.userMapper = userMapper;
        this.scoreMapper = scoreMapper;
    }

    /**
     * 创建并发布一场考试。
     *
     * <p>校验顺序有意如此：先校验试卷与班级存在，再校验时间窗与参数，
     * 最后才落库——任何一步不过都不该产生半成品数据。
     */
    @Transactional
    public long create(Exam input) {
        ExamPaper paper = paperMapper.selectById(input.getPaperId());
        if (paper == null) {
            throw BizException.notFound("试卷");
        }
        if (paper.getStatus() == null || paper.getStatus() != 2) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "试卷尚未发布，不能用于考试");
        }

        SchoolClass clazz = classMapper.selectById(input.getClassId());
        if (clazz == null) {
            throw BizException.notFound("班级");
        }

        validateTimeWindow(input, paper);
        validateParams(input, paper);

        input.setStatus(0);   // 未开始
        examMapper.insert(input);

        // 为班级每名学生建成绩行（缺考态）——见类注释
        List<User> students = studentsOf(input.getClassId());
        for (User s : students) {
            Score sc = new Score();
            sc.setStudentId(s.getId());
            sc.setClassId(input.getClassId());
            sc.setExamId(input.getId());
            sc.setStatus(Score.Status.ABSENT.code());
            scoreMapper.insert(sc);
        }
        return input.getId();
    }

    public Exam getById(long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) {
            throw BizException.notFound("考试");
        }
        return exam;
    }

    public List<Exam> listOfClass(long classId) {
        return examMapper.selectList(new LambdaQueryWrapper<Exam>()
                .eq(Exam::getClassId, classId)
                .orderByDesc(Exam::getStartAt));
    }

    /**
     * 应考/实考/缺考名单。
     *
     * <p>三类状态直接读成绩行：缺考=ABSENT、已交卷或已阅卷=实考、其余为应考未考。
     */
    public ExamRoster roster(long examId) {
        Exam exam = getById(examId);
        List<Score> rows = scoreMapper.selectList(new LambdaQueryWrapper<Score>()
                .eq(Score::getExamId, examId));

        List<RosterItem> expected = new java.util.ArrayList<>();
        List<RosterItem> attended = new java.util.ArrayList<>();
        List<RosterItem> absent = new java.util.ArrayList<>();

        for (Score r : rows) {
            RosterItem item = new RosterItem(r.getStudentId(), null, r.getStatus());
            if (r.getStatus() != null && r.getStatus() == Score.Status.ABSENT.code()) {
                absent.add(item);
            } else {
                attended.add(item);
            }
            expected.add(item);
        }
        return new ExamRoster(examId, expected.size(), attended.size(), absent.size(),
                expected, attended, absent);
    }

    /** 班级内的学生列表 */
    public List<User> studentsOf(long classId) {
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getClassId, classId)
                .eq(User::getRole, "STUDENT")
                .eq(User::getStatus, 1)
                .orderByAsc(User::getUserNo));
    }

    // ---------- 校验 ----------

    private void validateTimeWindow(Exam input, ExamPaper paper) {
        if (input.getStartAt() == null || input.getEndAt() == null) {
            throw BizException.param("必须指定开始与截止时间");
        }
        if (!input.getEndAt().isAfter(input.getStartAt())) {
            throw BizException.param("截止时间必须晚于开始时间");
        }
        if (input.getDuration() == null || input.getDuration() <= 0) {
            throw BizException.param("答题时长必须大于 0");
        }
        // 时长不能超过时间窗：否则学生可以跨截止时间继续答
        long windowMinutes = java.time.Duration.between(input.getStartAt(), input.getEndAt()).toMinutes();
        if (input.getDuration() > windowMinutes) {
            throw BizException.param(String.format(
                    "答题时长 %d 分钟超过了考试时间窗 %d 分钟", input.getDuration(), windowMinutes));
        }
    }

    private void validateParams(Exam input, ExamPaper paper) {
        if (input.getMaxAttempts() == null || input.getMaxAttempts() < 1) {
            throw BizException.param("每人可考次数至少为 1");
        }
        String rule = input.getScoreRule();
        if (rule != null && !List.of("HIGHEST", "LAST", "AVERAGE").contains(rule)) {
            throw BizException.param("成绩取值规则必须是 HIGHEST / LAST / AVERAGE 之一");
        }
        String mode = input.getPublishMode();
        if (mode != null && !List.of("IMMEDIATE", "DELAYED", "MANUAL").contains(mode)) {
            throw BizException.param("成绩发布模式必须是 IMMEDIATE / DELAYED / MANUAL 之一");
        }
        if (input.getPassLine() != null
                && paper.getTotalScore() != null
                && input.getPassLine() > paper.getTotalScore()) {
            throw BizException.param(String.format(
                    "及格线 %d 超过了试卷总分 %d", input.getPassLine(), paper.getTotalScore()));
        }
        if (input.getRetakePolicy() != null && input.getRetakePolicy() != 0 && input.getRetakePolicy() != 1) {
            throw BizException.param("补考策略只能是 0（不允许）或 1（允许）");
        }
    }

    /** 考试当前是否在时间窗内 */
    public boolean isOpen(Exam exam, LocalDateTime now) {
        return !now.isBefore(exam.getStartAt()) && now.isAfter(exam.getEndAt()) == false;
    }

    /** 名单条目 */
    public record RosterItem(Long studentId, String studentName, Integer status) {
    }

    /** 考试名单 */
    public record ExamRoster(long examId, int expected, int attended, int absent,
                             List<RosterItem> expectedList,
                             List<RosterItem> attendedList,
                             List<RosterItem> absentList) {
    }
}
