package com.exam.repository.mapper;

import com.exam.repository.entity.ExamAttempt;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** exam_attempt 表的数据访问 */
@Mapper
public interface ExamAttemptMapper extends BaseMapper<ExamAttempt> {

    /** 阅卷队列行：某次尝试对某道题的作答 */
    record MarkingRow(Long attemptId, Long studentId, String studentName,
                      String answer, Integer gained) {
    }

    /**
     * 取某场考试某道题的全部待阅/已阅记录。
     *
     * <p>按 attempt_id 升序，保证多人阅卷时看到的顺序一致。
     * studentName 一并查出来，是否暴露由 Service 层决定——
     * 数据层不承担匿名策略，否则实名复核时拿不到姓名。
     */
    @Select("""
            SELECT a.id AS attemptId, a.student_id AS studentId, u.name AS studentName,
                   i.answer AS answer, i.gained AS gained
            FROM exam_attempt a
            JOIN exam_attempt_item i ON i.attempt_id = a.id AND i.question_id = #{questionId}
            JOIN sys_user u ON u.id = a.student_id
            WHERE a.exam_id = #{examId} AND a.status IN (1, 2)
            ORDER BY a.id
            """)
    List<MarkingRow> selectMarkingRows(@Param("examId") long examId,
                                       @Param("questionId") long questionId);

    /**
     * 写入或更新某题的阅卷得分。幂等：同一 (attempt, question) 重复调用覆盖旧分，不累加。
     */
    @Insert("""
            INSERT INTO exam_attempt_item (attempt_id, question_id, gained, graded_by, graded_at)
            VALUES (#{attemptId}, #{questionId}, #{gained}, #{markerId}, NOW())
            ON DUPLICATE KEY UPDATE gained = #{gained}, graded_by = #{markerId}, graded_at = NOW()
            """)
    int upsertItemScore(@Param("attemptId") long attemptId,
                        @Param("questionId") long questionId,
                        @Param("gained") int gained,
                        @Param("markerId") long markerId);

    /** 刷新某次尝试的总分 */
    @Update("UPDATE exam_attempt SET score = #{score} WHERE id = #{attemptId}")
    int updateAttemptScore(@Param("attemptId") long attemptId, @Param("score") int score);

    /** 某次尝试的各题得分合计 */
    @Select("SELECT COALESCE(SUM(gained), 0) FROM exam_attempt_item WHERE attempt_id = #{attemptId}")
    int sumAttemptScore(@Param("attemptId") long attemptId);
}
