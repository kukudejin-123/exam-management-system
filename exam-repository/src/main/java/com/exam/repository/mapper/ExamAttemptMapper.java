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

    /** 按 考试 + 考生 + 第几次 定位一次尝试 */
    @Select("""
            SELECT * FROM exam_attempt
            WHERE exam_id = #{examId} AND student_id = #{studentId} AND attempt_no = #{attemptNo}
            """)
    com.exam.repository.entity.ExamAttempt selectByKey(@Param("examId") long examId,
                                                      @Param("studentId") long studentId,
                                                      @Param("attemptNo") int attemptNo);

    /** 该考生在某场考试已发起过几次尝试 */
    @Select("SELECT COUNT(*) FROM exam_attempt WHERE exam_id = #{examId} AND student_id = #{studentId}")
    int countAttempts(@Param("examId") long examId, @Param("studentId") long studentId);

    /** 该考生是否已有进行中的尝试（断网重连时复用同一次，不新开） */
    @Select("""
            SELECT * FROM exam_attempt
            WHERE exam_id = #{examId} AND student_id = #{studentId} AND status = 0
            ORDER BY id DESC LIMIT 1
            """)
    com.exam.repository.entity.ExamAttempt selectInProgress(@Param("examId") long examId,
                                                            @Param("studentId") long studentId);

    /**
     * 保存考生作答。只写 answer，不碰 gained——
     * 否则会把阅卷人打的分冲掉。
     */
    @Insert("""
            INSERT INTO exam_attempt_item (attempt_id, question_id, answer)
            VALUES (#{attemptId}, #{questionId}, #{answer})
            ON DUPLICATE KEY UPDATE answer = #{answer}
            """)
    int upsertAnswer(@Param("attemptId") long attemptId,
                     @Param("questionId") long questionId,
                     @Param("answer") String answer);

    /** 取某次尝试已保存的全部作答，供断网续答恢复 */
    @Select("""
            SELECT question_id AS questionId, answer AS answer
            FROM exam_attempt_item WHERE attempt_id = #{attemptId}
            """)
    List<AnswerRow> selectAnswers(@Param("attemptId") long attemptId);

    /** 已保存的作答 */
    record AnswerRow(Long questionId, String answer) {
    }

    /** 交卷：仅当仍处于进行中才更新，保证幂等 */
    @Update("""
            UPDATE exam_attempt
            SET status = #{status}, submit_at = NOW()
            WHERE id = #{attemptId} AND status = 0
            """)
    int submitIfInProgress(@Param("attemptId") long attemptId, @Param("status") int status);

    /**
     * 写入客观题自动判分结果。
     *
     * <p>与 upsertItemScore 分开：自动判分没有阅卷人，
     * 不该写 graded_by/graded_at，否则留痕会失真。
     */
    @Insert("""
            INSERT INTO exam_attempt_item (attempt_id, question_id, gained)
            VALUES (#{attemptId}, #{questionId}, #{gained})
            ON DUPLICATE KEY UPDATE gained = #{gained}
            """)
    int upsertAutoGained(@Param("attemptId") long attemptId,
                         @Param("questionId") long questionId,
                         @Param("gained") int gained);

    /** 刷新某次尝试的总分 */
    @Update("UPDATE exam_attempt SET score = #{score} WHERE id = #{attemptId}")
    int updateAttemptScore(@Param("attemptId") long attemptId, @Param("score") int score);

    /** 某次尝试的各题得分合计 */
    @Select("SELECT COALESCE(SUM(gained), 0) FROM exam_attempt_item WHERE attempt_id = #{attemptId}")
    int sumAttemptScore(@Param("attemptId") long attemptId);
}
