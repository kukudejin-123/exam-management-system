package com.exam.repository.mapper;

import com.exam.repository.entity.Exam;
import org.apache.ibatis.annotations.Mapper;

/** Exam 表的数据访问 */
@Mapper
public interface ExamMapper extends BaseMapper<Exam> {
}
