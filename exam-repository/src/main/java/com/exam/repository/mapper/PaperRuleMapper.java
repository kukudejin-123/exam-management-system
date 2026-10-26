package com.exam.repository.mapper;

import com.exam.repository.entity.PaperRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** paper_rule 表的数据访问 */
@Mapper
public interface PaperRuleMapper extends BaseMapper<PaperRule> {

    @Select("SELECT bank_id FROM paper_rule_bank WHERE rule_id = #{ruleId}")
    List<Long> selectBankIds(@Param("ruleId") long ruleId);
}
