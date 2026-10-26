package com.exam.service.marking;

import com.exam.common.constant.ErrorCode;
import com.exam.common.exception.BizException;
import com.exam.repository.entity.ExamAttempt;
import com.exam.repository.mapper.ExamAttemptMapper;
import com.exam.repository.mapper.ScoreMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 主观题人工阅卷。
 *
 * <p>对应 F-08-2 简答题阅卷队列与 F-08-3 阅卷进度。
 *
 * <p>三条设计约束（来自竞品分析与立项评审）：
 * 1. **按题分配**——同一道简答题整队列交给阅卷人，逐份打分，避免一人独断；
 * 2. **匿名评阅**——默认不返回考生姓名，降低主观偏见（Moodle 的做法）；
 *    实名复核需显式开启，且仅管理员/组长可用；
 * 3. **幂等回填**——同一题重复打分覆盖旧分，不累加。
 */
@Service
public class MarkingService {

    private final ExamAttemptMapper attemptMapper;
    private final ScoreMapper scoreMapper;

    public MarkingService(ExamAttemptMapper attemptMapper, ScoreMapper scoreMapper) {
        this.attemptMapper = attemptMapper;
        this.scoreMapper = scoreMapper;
    }

    /**
     * 取某场考试某道简答题的阅卷队列。
     *
     * @param revealNames 是否显示考生姓名。默认 false（匿名评阅）
     */
    public List<MarkingItem> queue(long examId, long questionId, boolean revealNames) {
        return attemptMapper.selectMarkingRows(examId, questionId).stream()
                .map(row -> new MarkingItem(
                        row.attemptId(),
                        row.studentId(),
                        revealNames ? row.studentName() : null,
                        row.answer(),
                        row.gained()))
                .filter(item -> item.answer() != null && !item.answer().isBlank())
                .toList();
    }

    /**
     * 给一道简答题打分。
     *
     * <p>幂等：同一 (attempt, question) 重复打分覆盖旧分，不累加。
     * 打分后同步刷新该次尝试总分与成绩表。
     */
    @Transactional
    public void grade(long attemptId, long questionId, int gained, long markerId) {
        if (gained < 0) {
            throw BizException.param("得分不能为负数");
        }
        ExamAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw BizException.notFound("作答记录");
        }
        if (attempt.getStatus() == null || attempt.getStatus() == 0) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "考生尚未交卷，不能阅卷");
        }

        int rows = attemptMapper.upsertItemScore(attemptId, questionId, gained, markerId);
        if (rows == 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "该次尝试没有这道题的作答记录");
        }

        // 刷新本次尝试总分：各题得分之和
        int total = attemptMapper.sumAttemptScore(attemptId);
        attemptMapper.updateAttemptScore(attemptId, total);

        // 同步成绩表（按考试 + 考生定位）
        scoreMapper.syncFromMarking(attempt.getExamId(), attempt.getStudentId(), total, markerId);
    }

    /**
     * 阅卷进度：已阅 / 待阅。
     */
    public MarkingProgress progress(long examId, long questionId) {
        List<MarkingItem> all = queue(examId, questionId, false);
        int graded = (int) all.stream().filter(MarkingItem::graded).count();
        return new MarkingProgress(all.size(), graded, all.size() - graded);
    }

}
