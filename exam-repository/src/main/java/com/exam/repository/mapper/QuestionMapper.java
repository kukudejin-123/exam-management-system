package com.exam.repository.mapper;

import com.exam.repository.entity.Question;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** question 表的数据访问 */
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * 批量插入题目，用于题库 Excel 导入。空列表直接返回 0。
     *
     * @return 实际插入行数
     */
    int insertBatch(@Param("list") List<Question> questions);
}
