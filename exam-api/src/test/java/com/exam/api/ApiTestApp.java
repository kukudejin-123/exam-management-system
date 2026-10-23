package com.exam.api;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * api 模块测试用的应用上下文。
 *
 * <p>Service 全部由 TestBeans 打桩，所以不需要 MapperScan，
 * 也不连数据库——数据访问已由 exam-repository 的集成测试覆盖。
 */
@SpringBootApplication(scanBasePackages = {"com.exam.api", "com.exam.security", "com.exam.service"})
@Import(com.exam.api.TestBeans.class)
@org.springframework.boot.autoconfigure.EnableAutoConfiguration(
        exclude = {DataSourceAutoConfiguration.class,
                   DataSourceTransactionManagerAutoConfiguration.class,
                   })
public class ApiTestApp {
}
