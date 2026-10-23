package com.exam.repository.mapper;

import com.exam.repository.entity.QuestionBank;
import org.apache.ibatis.annotations.Mapper;

/** QuestionBank 表的数据访问 */
@Mapper
public interface QuestionBankMapper extends BaseMapper<QuestionBank> {
}
