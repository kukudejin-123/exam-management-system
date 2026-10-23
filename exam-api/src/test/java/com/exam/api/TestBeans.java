package com.exam.api;

import com.exam.service.auth.AuthService;
import com.exam.service.clazz.ClassService;
import com.exam.service.grading.GradingService;
import com.exam.service.question.QuestionService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Web 层测试的桩配置。
 *
 * <p>不用 @MockBean/@MockitoBean——Spring Boot 3.2 配套的 spring-test 版本
 * 与 spring-security-test 之间存在版本冲突，强升 spring-test 又会和 spring-core
 * 不兼容。用 @TestConfiguration 提供 @Primary 的 Mockito mock，
 * 语义完全一样，且不碰版本解析。
 */
@TestConfiguration
public class TestBeans {

    @Bean
    @Primary
    public ClassService classService() {
        return Mockito.mock(ClassService.class);
    }

    @Bean
    @Primary
    public QuestionService questionService() {
        return Mockito.mock(QuestionService.class);
    }

    @Bean
    @Primary
    public AuthService authService() {
        return Mockito.mock(AuthService.class);
    }

    @Bean
    @Primary
    public GradingService gradingService() {
        return Mockito.mock(GradingService.class);
    }
}
