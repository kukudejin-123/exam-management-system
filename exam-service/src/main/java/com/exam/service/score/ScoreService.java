package com.exam.service.score;

import com.exam.common.constant.ErrorCode;
import com.exam.common.exception.BizException;
import com.exam.repository.entity.Exam;
import com.exam.repository.entity.ExamAttempt;
import com.exam.repository.entity.OperationLog;
import com.exam.repository.entity.Score;
import com.exam.repository.mapper.ExamAttemptMapper;
import com.exam.repository.mapper.ExamMapper;
import com.exam.repository.mapper.OperationLogMapper;
import com.exam.repository.mapper.ScoreMapper;
import com.exam.repository.mapper.UserMapper;
import com.exam.service.grading.GradingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 成绩管理。
 *
 * <p>对应 F-09-1 成绩册、F-09-2 正考补考汇总、F-09-3 修改留痕、F-09-4 学生查分。
 *
 * <p>**总评口径**（写清楚，避免歧义）：
 * 只统计学生**实际参考**的考试。缺考的考试既不进分子也不进分母——
 * 因为缺考按设计是走补考的，补考结果会作为另一次参考计入。
 * 若把缺考按 0 分计入，一个因故缺考一场的学生总评会被无意义拉低。
 */
@Service
public class ScoreService {

    private final ScoreMapper scoreMapper;
    private final ExamMapper examMapper;
    private final ExamAttemptMapper attemptMapper;
    private final UserMapper userMapper;
    private final OperationLogMapper logMapper;
    private final GradingService gradingService;
    private final ObjectMapper json;

    public ScoreService(ScoreMapper scoreMapper,
                        ExamMapper examMapper,
                        ExamAttemptMapper attemptMapper,
                        UserMapper userMapper,
                        OperationLogMapper logMapper,
                        GradingService gradingService,
                        ObjectMapper json) {
        this.scoreMapper = scoreMapper;
        this.examMapper = examMapper;
        this.attemptMapper = attemptMapper;
        this.userMapper = userMapper;
        this.logMapper = logMapper;
        this.gradingService = gradingService;
        this.json = json;
    }

    // ---------- F-09-1 成绩册 ----------

    /**
     * 班级成绩册。
     *
     * <p>三态区分是验收标准明确要求的：缺考 / 0 分 / 未阅卷必须能分开，
     * 不能都显示成一个数字。
     */
    public GradeBook gradeBook(long examId, String sortBy, String statusFilter) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw BizException.notFound("考试");
        }

        List<Score> rows = scoreMapper.selectList(new LambdaQueryWrapper<Score>()
                .eq(Score::getExamId, examId));

        List<GradeBookRow> items = new ArrayList<>();
        for (Score r : rows) {
            var user = userMapper.selectById(r.getStudentId());
            GradeBookRow row = new GradeBookRow(
                    r.getId(), r.getStudentId(),
                    user == null ? null : user.getUserNo(),
                    user == null ? null : user.getName(),
                    r.getRawScore(), r.getFinalScore(), r.getPassed(), r.getStatus());
            if (statusFilter != null && !statusFilter.isBlank()
                    && !statusFilter.equals(String.valueOf(r.getStatus()))) {
                continue;
            }
            items.add(row);
        }

        sortRows(items, sortBy);

        int pass = 0;
        int fail = 0;
        int absent = 0;
        int ungraded = 0;
        for (GradeBookRow r : items) {
            switch (r.status() == null ? 0 : r.status()) {
                case 2 -> absent++;
                case 4 -> ungraded++;
                case 3 -> fail++;          // 0 分即不及格
                default -> {
                    // 及格与否由分数对比及格线现算，不依赖 passed 字段——
                    // 那个字段目前没有写入方，读它会永远得到 null
                    if (isPassed(r, exam)) {
                        pass++;
                    } else {
                        fail++;
                    }
                }
            }
        }
        return new GradeBook(examId, exam.getName(), items.size(), pass, fail, absent, ungraded, items);
    }

    // ---------- F-09-2 总评 ----------

    /**
     * 班级总评：按考试权重加权平均。
     *
     * <p>只统计实际参考的考试（见类注释的口径说明）。
     */
    public List<FinalScoreRow> finalScores(long classId) {
        List<Exam> exams = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                .eq(Exam::getClassId, classId)
                .gt(Exam::getWeight, 0)
                .orderByAsc(Exam::getStartAt));
        if (exams.isEmpty()) {
            return List.of();
        }

        // 学生 -> 考试 -> 成绩
        Map<Long, Map<Long, Score>> byStudent = new LinkedHashMap<>();
        for (Exam exam : exams) {
            for (Score s : scoreMapper.selectList(new LambdaQueryWrapper<Score>()
                    .eq(Score::getExamId, exam.getId()))) {
                if (s.getStatus() != null && s.getStatus() == Score.Status.ABSENT.code()) {
                    continue;   // 缺考不计入
                }
                byStudent.computeIfAbsent(s.getStudentId(), k -> new LinkedHashMap<>())
                        .put(exam.getId(), s);
            }
        }

        List<FinalScoreRow> result = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, Score>> e : byStudent.entrySet()) {
            long studentId = e.getKey();
            var user = userMapper.selectById(studentId);
            double weighted = 0;
            int weightSum = 0;
            List<ExamScore> details = new ArrayList<>();

            for (Exam exam : exams) {
                Score s = e.getValue().get(exam.getId());
                if (s == null) {
                    // 缺考或未参考：分子分母都不计。若只跳过分子仍保留权重，
                    // 一场缺考会把总评无意义地拉低一半。
                    continue;
                }
                int score = s.getFinalScore() == null ? 0 : s.getFinalScore();
                int weight = exam.getWeight() == null ? 1 : exam.getWeight();
                weighted += (double) score * weight;
                weightSum += weight;
                details.add(new ExamScore(exam.getId(), exam.getName(), score, weight));
            }

            if (weightSum == 0) {
                continue;   // 全部缺考，不产生总评行
            }

            int finalScore = weightSum == 0 ? 0 : (int) Math.round(weighted / weightSum);
            result.add(new FinalScoreRow(studentId,
                    user == null ? null : user.getUserNo(),
                    user == null ? null : user.getName(),
                    finalScore, finalScore >= 60, details));
        }

        result.sort((a, b) -> Integer.compare(b.finalScore(), a.finalScore()));
        return result;
    }

    // ---------- F-09-3 修改留痕 ----------

    /**
     * 修改成绩。必须留痕：谁、何时、改前改后。
     *
     * <p>先写日志再改数据，同一个事务里——日志写失败则修改回滚，
     * 不会出现"改了但没记录"的情况。
     */
    @Transactional
    public void updateScore(long scoreId, int newScore, long operatorId) {
        Score score = scoreMapper.selectById(scoreId);
        if (score == null) {
            throw BizException.notFound("成绩");
        }
        if (score.getStatus() != null && score.getStatus() == Score.Status.ABSENT.code()) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "缺考成绩不能直接修改，请先安排补考");
        }

        int before = score.getFinalScore() == null ? 0 : score.getFinalScore();
        if (before == newScore) {
            return;   // 没有变化，不写日志
        }

        writeLog(operatorId, "SCORE_UPDATE", "score:" + scoreId,
                Map.of("finalScore", before), Map.of("finalScore", newScore));

        score.setRawScore(newScore);
        score.setFinalScore(newScore);
        score.setGradedBy(operatorId);
        score.setUpdatedAt(LocalDateTime.now());
        scoreMapper.updateById(score);
    }

    // ---------- F-09-4 学生查分 ----------

    /** 学生查自己的成绩。只能看自己的，越权直接拒 */
    public List<MyScoreRow> myScores(long studentId, long operatorId) {
        if (studentId != operatorId) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能查询自己的成绩");
        }
        List<Score> rows = scoreMapper.selectList(new LambdaQueryWrapper<Score>()
                .eq(Score::getStudentId, studentId)
                .orderByDesc(Score::getUpdatedAt));

        List<MyScoreRow> result = new ArrayList<>();
        for (Score r : rows) {
            Exam exam = examMapper.selectById(r.getExamId());
            result.add(new MyScoreRow(
                    r.getExamId(),
                    exam == null ? null : exam.getName(),
                    exam == null ? null : exam.getStartAt(),
                    r.getFinalScore(), r.getPassed(), r.getStatus()));
        }
        return result;
    }

    // ---------- 内部 ----------

    /** 是否及格。未设备格线时一律及格；分数为 null（未阅卷）不算及格 */
    private boolean isPassed(GradeBookRow row, Exam exam) {
        if (row.finalScore() == null) {
            return false;
        }
        if (exam.getPassLine() == null) {
            return true;
        }
        return row.finalScore() >= exam.getPassLine();
    }

    private void sortRows(List<GradeBookRow> items, String sortBy) {
        if (sortBy == null || sortBy.isBlank() || "score".equals(sortBy)) {
            items.sort((a, b) -> {
                int sa = a.finalScore() == null ? -1 : a.finalScore();
                int sb = b.finalScore() == null ? -1 : b.finalScore();
                return Integer.compare(sb, sa);
            });
        } else if ("userNo".equals(sortBy)) {
            items.sort((a, b) -> {
                if (a.userNo() == null) {
                    return 1;
                }
                if (b.userNo() == null) {
                    return -1;
                }
                return a.userNo().compareTo(b.userNo());
            });
        }
    }

    private void writeLog(long userId, String action, String target,
                          Map<String, Object> before, Map<String, Object> after) {
        try {
            OperationLog log = new OperationLog();
            log.setUserId(userId);
            log.setAction(action);
            log.setTarget(target);
            log.setBeforeVal(json.writeValueAsString(before));
            log.setAfterVal(json.writeValueAsString(after));
            logMapper.insert(log);
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "操作日志写入失败，修改已取消");
        }
    }

    /** 成绩册行 */
    public record GradeBookRow(Long scoreId, Long studentId, String userNo, String name,
                               Integer rawScore, Integer finalScore, Integer passed,
                               Integer status) {

        /** 状态的中文说明，避免前端硬编码数字 */
        public String statusLabel() {
            return switch (status == null ? 0 : status) {
                case 2 -> "缺考";
                case 3 -> "0分";
                case 4 -> "未阅卷";
                default -> "正常";
            };
        }
    }

    /** 成绩册 */
    public record GradeBook(long examId, String examName, int total, int pass, int fail,
                            int absent, int ungraded, List<GradeBookRow> rows) {
    }

    /** 总评行 */
    public record FinalScoreRow(long studentId, String userNo, String name,
                                int finalScore, boolean passed, List<ExamScore> details) {
    }

    /** 单场考试在总评中的贡献 */
    public record ExamScore(long examId, String examName, int score, int weight) {
    }

    /** 学生视角的成绩 */
    public record MyScoreRow(long examId, String examName, LocalDateTime startAt,
                             Integer finalScore, Integer passed, Integer status) {

        public String statusLabel() {
            return switch (status == null ? 0 : status) {
                case 2 -> "缺考";
                case 3 -> "0分";
                case 4 -> "未阅卷";
                default -> "正常";
            };
        }
    }
}
