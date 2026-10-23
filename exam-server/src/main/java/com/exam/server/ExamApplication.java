package com.exam.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 考试管理系统启动类。
 *
 * <p>启动模块只做装配：扫描各业务模块的组件、注册 Mapper、打开 Swagger。
 * 业务实现一律放在 exam-service 及以下，这里不放任何业务代码。
 */
@SpringBootApplication(scanBasePackages = "com.exam")
@MapperScan("com.exam.repository.mapper")
public class ExamApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamApplication.class, args);
    }
}
