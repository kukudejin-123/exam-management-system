package com.exam.repository.mapper;

import com.exam.repository.entity.Score;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** score 表的数据访问 */
@Mapper
public interface ScoreMapper extends BaseMapper<Score> {

    /**
     * 阅卷后同步成绩：按 考试 + 考生 定位，回填得分与阅卷人。
     *
     * <p>用 update 而非 insert：成绩行在发布考试时就该建好（缺考也占一行），
     * 阅卷只是改状态与分数。行不存在时更新 0 行，由调用方判断。
     */
    @Update("""
            UPDATE score
            SET raw_score = #{score}, final_score = #{score}, status = 1,
                graded_by = #{markerId}, updated_at = NOW()
            WHERE exam_id = #{examId} AND student_id = #{studentId}
            """)
    int syncFromMarking(@Param("examId") long examId,
                        @Param("studentId") long studentId,
                        @Param("score") int score,
                        @Param("markerId") long markerId);
}
