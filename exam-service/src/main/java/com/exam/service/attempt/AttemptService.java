package com.exam.service.attempt;

import com.exam.common.constant.ErrorCode;
import com.exam.common.exception.BizException;
import com.exam.repository.entity.Exam;
import com.exam.repository.entity.ExamAttempt;
import com.exam.repository.mapper.ExamAttemptMapper;
import com.exam.repository.mapper.ExamMapper;
import com.exam.repository.mapper.ExamPaperMapper;
import com.exam.service.grading.GradingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 在线答题。
 *
 * <p>对应 F-06-1 考生入场、F-06-2 断网续答与服务端倒计时、F-06-4 交卷幂等。
 *
 * <p>**断网续答的实现方式**：
 * 1. 每次作答立即 upsert 落库，不等交卷——本地缓存只是优化，库才是真相；
 * 2. 重连后按 (exam, student) 复用**同一次**进行中的尝试，不新开，
 *    所以刷新页面、断网重连、换设备都不丢进度；
 * 3. 倒计时由服务端算：截止时刻 = min(开始时刻 + 时长, 考试截止时刻)，
 *    客户端只展示，不信任客户端时间。
 *
 * <p>**交卷幂等**：submitIfInProgress 带 status=0 条件，已交卷的再调一次
 * 影响 0 行，直接返回已有成绩，不会重复判分。
 *
 * <p>**答案不外泄**：给学生的卷面走 selectPaperQuestionsForStudent（不含 answer 列），
 * 判分走 selectPaperQuestions（含 answer）。两者分开是安全要求，不是优化。
 */
@Service
public class AttemptService {

    private final ExamMapper examMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamAttemptMapper attemptMapper;
    private final GradingService gradingService;

    public AttemptService(ExamMapper examMapper,
                          ExamPaperMapper paperMapper,
                          ExamAttemptMapper attemptMapper,
                          GradingService gradingService) {
        this.examMapper = examMapper;
        this.paperMapper = paperMapper;
        this.attemptMapper = attemptMapper;
        this.gradingService = gradingService;
    }

    /**
     * 考生入场。
     *
     * <p>已有进行中的尝试就复用它（断网重连的核心）；
     * 否则检查次数上限后新开一次。
     */
    @Transactional
    public ExamAttempt enter(long examId, long studentId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw BizException.notFound("考试");
        }
        if (!isOpen(exam, LocalDateTime.now())) {
            throw new BizException(ErrorCode.EXAM_TIME_CONFLICT, "当前不在考试时间范围内");
        }

        ExamAttempt inProgress = attemptMapper.selectInProgress(examId, studentId);
        if (inProgress != null) {
            return inProgress;
        }

        int used = attemptMapper.countAttempts(examId, studentId);
        int max = exam.getMaxAttempts() == null ? 1 : exam.getMaxAttempts();
        if (used >= max) {
            throw new BizException(ErrorCode.STATE_CONFLICT,
                    String.format("可考次数已用完（限 %d 次，已用 %d 次）", max, used));
        }

        ExamAttempt attempt = new ExamAttempt();
        attempt.setExamId(examId);
        attempt.setStudentId(studentId);
        attempt.setAttemptNo(used + 1);
        attempt.setStatus(0);
        attempt.setStartedAt(LocalDateTime.now());
        attemptMapper.insert(attempt);
        return attempt;
    }

    /**
     * 取本次尝试的卷面与学生已保存的作答。
     *
     * <p>卷面不含答案；已保存作答用于恢复现场。
     */
    public AttemptPaper paper(long attemptId) {
        ExamAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw BizException.notFound("作答记录");
        }
        Exam exam = examMapper.selectById(attempt.getExamId());
        if (exam == null) {
            throw BizException.notFound("考试");
        }

        Map<Long, String> saved = savedAnswers(attemptId);
        List<AttemptQuestion> questions = paperMapper.selectPaperQuestionsForStudent(exam.getPaperId())
                .stream()
                .map(q -> new AttemptQuestion(q.questionId(), q.seq(), q.score(), q.type(),
                        q.stem(), q.options(), saved.get(q.questionId())))
                .toList();

        return new AttemptPaper(attemptId, attempt.getAttemptNo(), exam.getName(),
                exam.getDuration(), remainingSeconds(attempt, exam), questions);
    }

    /**
     * 保存一道题的作答。
     *
     * <p>超时后拒绝保存——必须走交卷，否则学生可以一直答下去。
     */
    @Transactional
    public void saveAnswer(long attemptId, long questionId, String answer) {
        requireOpen(attemptId);
        attemptMapper.upsertAnswer(attemptId, questionId, answer);
    }

    /**
     * 交卷。幂等：已交卷的再调一次返回已有成绩，不重复判分。
     */
    @Transactional
    public SubmitResult submit(long attemptId) {
        ExamAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw BizException.notFound("作答记录");
        }
        if (attempt.getStatus() != null && attempt.getStatus() != 0) {
            return new SubmitResult(attempt.getScore(), attempt.getStatus(), false);
        }

        Exam exam = examMapper.selectById(attempt.getExamId());
        int total = gradeObjective(attempt, exam);

        // 带 status=0 条件的更新：并发下只有一个能成功
        int rows = attemptMapper.submitIfInProgress(attemptId, 1);
        if (rows == 0) {
            ExamAttempt current = attemptMapper.selectById(attemptId);
            return new SubmitResult(current.getScore(), current.getStatus(), false);
        }

        attemptMapper.updateAttemptScore(attemptId, total);
        return new SubmitResult(total, 1, true);
    }

    /**
     * 剩余秒数。以服务端时间为准，截止时刻取
     * min(开始 + 时长, 考试截止) 中较早的那个。
     */
    public long remainingSeconds(ExamAttempt attempt, Exam exam) {
        LocalDateTime byDuration = attempt.getStartedAt()
                .plusMinutes(exam.getDuration() == null ? 0 : exam.getDuration());
        LocalDateTime deadline = byDuration.isBefore(exam.getEndAt()) ? byDuration : exam.getEndAt();
        return Math.max(Duration.between(LocalDateTime.now(), deadline).toSeconds(), 0);
    }

    /** 考试是否在时间窗内 */
    public boolean isOpen(Exam exam, LocalDateTime now) {
        return !now.isBefore(exam.getStartAt()) && now.isBefore(exam.getEndAt());
    }

    // ---------- 内部 ----------

    private Map<Long, String> savedAnswers(long attemptId) {
        Map<Long, String> saved = new HashMap<>();
        for (ExamAttemptMapper.AnswerRow row : attemptMapper.selectAnswers(attemptId)) {
            saved.put(row.questionId(), row.answer());
        }
        return saved;
    }

    /** 取一次仍可作答的尝试：必须存在、未交卷、未超时 */
    private ExamAttempt requireOpen(long attemptId) {
        ExamAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw BizException.notFound("作答记录");
        }
        if (attempt.getStatus() != null && attempt.getStatus() != 0) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "已交卷，不能再作答");
        }
        Exam exam = examMapper.selectById(attempt.getExamId());
        if (remainingSeconds(attempt, exam) <= 0) {
            submit(attemptId);
            throw new BizException(ErrorCode.EXAM_TIME_CONFLICT, "答题时间已到，已自动交卷");
        }
        return attempt;
    }

    /** 客观题自动判分，返回本次尝试总分 */
    private int gradeObjective(ExamAttempt attempt, Exam exam) {
        Map<Long, String> answers = savedAnswers(attempt.getId());
        int total = 0;

        for (ExamPaperMapper.PaperQuestionRow q : paperMapper.selectPaperQuestions(exam.getPaperId())) {
            if ("SHORT".equals(q.type())) {
                continue;   // 主观题走人工阅卷
            }
            String answer = answers.get(q.questionId());
            if (answer == null) {
                continue;   // 未作答按 0 分
            }
            Integer gained = gradingService.gradeByType(q.type(), q.answer(), answer, q.score(), true);
            if (gained != null) {
                attemptMapper.upsertAutoGained(attempt.getId(), q.questionId(), gained);
                total += gained;
            }
        }
        return total;
    }

}
