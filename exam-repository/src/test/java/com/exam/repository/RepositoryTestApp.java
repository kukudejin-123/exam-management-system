package com.exam.repository;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * repository 模块测试用的最小 Spring 上下文。
 *
 * <p>不引入 exam-server 的完整自动配置——数据访问层的测试只需要
 * MyBatis-Plus 与数据源，启动越轻，失败原因越明确。
 */
@SpringBootApplication
@MapperScan("com.exam.repository.mapper")
public class RepositoryTestApp {
}
