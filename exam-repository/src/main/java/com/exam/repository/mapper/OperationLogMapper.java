package com.exam.repository.mapper;

import com.exam.repository.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/** sys_operation_log 表的数据访问 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
