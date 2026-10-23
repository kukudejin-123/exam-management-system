package com.exam.repository.mapper;

import com.exam.repository.entity.User;
import org.apache.ibatis.annotations.Mapper;

/** sys_user 表的数据访问 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
