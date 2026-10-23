package com.exam.repository.mapper;

import com.exam.repository.entity.Score;
import org.apache.ibatis.annotations.Mapper;

/** Score 表的数据访问 */
@Mapper
public interface ScoreMapper extends BaseMapper<Score> {
}
