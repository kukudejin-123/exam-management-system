package com.exam.repository.mapper;

/**
 * 所有 Mapper 的基础接口。
 *
 * <p>MyBatis-Plus 的 BaseMapper 已提供 insert / deleteById / updateById / selectById
 * 与 Lambda 条件构造器，覆盖了绝大部分单表 CRUD。批量能力按需在各实体的 Mapper
 * 上单独声明（见 {@link QuestionMapper#insertBatch}），不放在通用基类里——
 * 通用批量 SQL 无法在不知道列名的情况下写对。
 *
 * @param <T> 实体类型
 */
public interface BaseMapper<T> extends com.baomidou.mybatisplus.core.mapper.BaseMapper<T> {
}
