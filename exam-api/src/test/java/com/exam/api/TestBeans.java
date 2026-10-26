package com.exam.api;

import com.exam.repository.mapper.ExamAttemptMapper;
import com.exam.repository.mapper.ExamMapper;
import com.exam.repository.mapper.ExamPaperMapper;
import com.exam.repository.mapper.PaperRuleMapper;
import com.exam.repository.mapper.QuestionMapper;
import com.exam.repository.mapper.ScoreMapper;
import com.exam.repository.mapper.SchoolClassMapper;
import com.exam.repository.mapper.UserMapper;
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

    // 各 Service 直接依赖的 Mapper。@SpringBootApplication 没扫 repository 包，
    // 这里逐个打桩，避免为 Web 层测试拉起数据源。
    @Bean
    @Primary
    public UserMapper userMapper() {
        return Mockito.mock(UserMapper.class);
    }

    @Bean
    @Primary
    public SchoolClassMapper schoolClassMapper() {
        return Mockito.mock(SchoolClassMapper.class);
    }

    @Bean
    @Primary
    public QuestionMapper questionMapper() {
        return Mockito.mock(QuestionMapper.class);
    }

    @Bean
    @Primary
    public ExamPaperMapper examPaperMapper() {
        return Mockito.mock(ExamPaperMapper.class);
    }

    @Bean
    @Primary
    public PaperRuleMapper paperRuleMapper() {
        return Mockito.mock(PaperRuleMapper.class);
    }

    @Bean
    @Primary
    public ExamMapper examMapper() {
        return Mockito.mock(ExamMapper.class);
    }

    @Bean
    @Primary
    public ExamAttemptMapper examAttemptMapper() {
        return Mockito.mock(ExamAttemptMapper.class);
    }

    @Bean
    @Primary
    public ScoreMapper scoreMapper() {
        return Mockito.mock(ScoreMapper.class);
    }
}
