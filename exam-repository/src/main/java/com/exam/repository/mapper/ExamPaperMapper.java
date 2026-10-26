package com.exam.repository.mapper;

import com.exam.repository.entity.ExamPaper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** exam_paper 表的数据访问 */
@Mapper
public interface ExamPaperMapper extends BaseMapper<ExamPaper> {

    /** 试卷-题目关联行 */
    record PaperQuestionRow(Long questionId, int seq, int score, String type,
                            String stem, String options, String answer) {
    }

    @Insert("""
            INSERT INTO exam_paper_question (paper_id, question_id, seq, score)
            VALUES (#{paperId}, #{questionId}, #{seq}, #{score})
            """)
    int insertPaperQuestion(@Param("paperId") long paperId,
                            @Param("questionId") long questionId,
                            @Param("seq") int seq,
                            @Param("score") int score);

    @Select("""
            SELECT q.id AS questionId, pq.seq AS seq, pq.score AS score,
                   q.type AS type, q.stem AS stem, q.options AS options, q.answer AS answer
            FROM exam_paper_question pq
            JOIN question q ON q.id = pq.question_id
            WHERE pq.paper_id = #{paperId} AND q.deleted = 0
            ORDER BY pq.seq
            """)
    List<PaperQuestionRow> selectPaperQuestions(@Param("paperId") long paperId);

    /**
     * 学生取卷：**不含 answer 列**。
     *
     * <p>把答案一起返回等于把试卷答案泄露给考生，这是安全缺陷而非优化项。
     */
    @Select("""
            SELECT q.id AS questionId, pq.seq AS seq, pq.score AS score,
                   q.type AS type, q.stem AS stem, q.options AS options,
                   NULL AS answer
            FROM exam_paper_question pq
            JOIN question q ON q.id = pq.question_id
            WHERE pq.paper_id = #{paperId} AND q.deleted = 0
            ORDER BY pq.seq
            """)
    List<PaperQuestionRow> selectPaperQuestionsForStudent(@Param("paperId") long paperId);

    @Select("SELECT COALESCE(SUM(score), 0) FROM exam_paper_question WHERE paper_id = #{paperId}")
    int sumQuestionScore(@Param("paperId") long paperId);
}
