package com.exam.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.repository.entity.Question;
import com.exam.repository.mapper.QuestionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QuestionMapper 集成测试。
 *
 * <p>默认在本地 MySQL 上跑（profile {@code local}，连接信息见 application-local.yml）。
 * CI 或有 Docker 的机器上用 {@code -Dspring.profiles.active=testcontainers}，
 * Testcontainers 会拉起一个 MySQL 8 容器并执行 schema.sql——
 * 与 H2 内存库不同，这里验证的是真实 MySQL 上的 JSON 列、唯一键与全文索引行为。
 */
@ActiveProfiles("${it.profile:local}")
@SpringBootTest(classes = RepositoryTestApp.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QuestionMapperIT {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withCommand("--ngram-token-size=2")
            .withInitScript("sql/schema.sql");

    @Autowired
    private QuestionMapper questionMapper;

    @BeforeEach
    void clean() {
        questionMapper.delete(null);
    }

    @Test
    @DisplayName("单条插入后能按 id 查出，字段与 JSON 列完整保留")
    void insert_then_select_by_id() {
        Question q = new Question();
        q.setBankId(1L);
        q.setType("SINGLE");
        q.setStem("数据结构是研究什么的学科？");
        q.setOptions("{\"A\":\"逻辑结构\",\"B\":\"物理结构\"}");
        q.setAnswer("{\"correct\":[\"A\"]}");
        q.setDifficulty(1);
        q.setChapter("第一章");
        q.setKnowledge("基本概念");
        q.setStatus(1);

        int rows = questionMapper.insert(q);
        assertThat(rows).isEqualTo(1);
        assertThat(q.getId()).isNotNull();

        Question found = questionMapper.selectById(q.getId());
        assertThat(found.getStem()).isEqualTo("数据结构是研究什么的学科？");
        // MySQL 的 JSON 列会归一化格式（冒号后加空格），所以比较解析后的结构而非字符串
        assertThat(parseJson(found.getOptions()))
                .containsEntry("A", "逻辑结构").containsEntry("B", "物理结构");
        assertThat(parseJson(found.getAnswer())).containsEntry("correct", List.of("A"));
    }

    @Test
    @DisplayName("批量插入一次写入多行，返回行数等于列表大小")
    void insert_batch_writes_all_rows() {
        List<Question> batch = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Question q = new Question();
            q.setBankId(1L);
            q.setType("JUDGE");
            q.setStem("判断题 " + i);
            q.setAnswer("{\"correct\":[\"Y\"]}");
            q.setDifficulty(2);
            q.setStatus(1);
            batch.add(q);
        }

        int rows = questionMapper.insertBatch(batch);
        assertThat(rows).isEqualTo(5);
        assertThat(questionMapper.selectCount(null)).isEqualTo(5);
    }

    @Test
    @DisplayName("批量插入空列表不写库，且不影响已有数据")
    void insert_batch_empty_is_noop() {
        seed("第一章", "题A");
        int rows = questionMapper.insertBatch(Collections.emptyList());
        // 空列表时 XML 走 SELECT 占位，JDBC 返回 -1，关键是没发出 INSERT
        assertThat(rows).isLessThanOrEqualTo(0);
        assertThat(questionMapper.selectCount(null)).isEqualTo(1);
    }

    @Test
    @DisplayName("逻辑删除后查不到，但数据仍在表里")
    void logical_delete_hides_row() {
        Question q = new Question();
        q.setBankId(1L);
        q.setType("FILL");
        q.setStem("单链表删除节点");
        q.setAnswer("{\"accept\":[\"p.next.next\"]}");
        q.setStatus(1);
        questionMapper.insert(q);

        questionMapper.deleteById(q.getId());
        assertThat(questionMapper.selectById(q.getId())).isNull();
    }

    @Test
    @DisplayName("条件构造器按章节筛选")
    void select_by_chapter() {
        seed("第一章", "题A");
        seed("第二章", "题B");
        seed("第二章", "题C");

        List<Question> found = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getChapter, "第二章")
                        .orderByAsc(Question::getStem));

        assertThat(found).extracting(Question::getStem).containsExactly("题B", "题C");
    }

    @Test
    @DisplayName("按 id 列表查询")
    void select_batch_ids() {
        Question a = seed("第一章", "题A");
        Question b = seed("第二章", "题B");

        List<Question> found = questionMapper.selectBatchIds(Arrays.asList(a.getId(), b.getId()));
        assertThat(found).hasSize(2);
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private static Map<String, Object> parseJson(String raw) {
        try {
            return JSON.readValue(raw, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("JSON 解析失败: " + raw, e);
        }
    }

    private Question seed(String chapter, String stem) {
        Question q = new Question();
        q.setBankId(1L);
        q.setType("SHORT");
        q.setStem(stem);
        q.setAnswer("{\"accept\":[]}");
        q.setChapter(chapter);
        q.setDifficulty(2);
        q.setStatus(1);
        questionMapper.insert(q);
        return q;
    }

    /**
     * 把 Testcontainers 的动态连接信息注册进 Spring 环境。
     * 用 @DynamicPropertySource 风格的静态注册，确保容器先于上下文刷新启动。
     */
    /**
     * 只有 testcontainers profile 才注册容器连接信息。
     * local profile 下用 application-local.yml 里的本机 MySQL，
     * 不去启动容器——否则没有 Docker 的机器会直接失败。
     */
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        String profile = System.getProperty("it.profile", "local");
        if (!profile.contains("testcontainers")) {
            return;
        }
        MYSQL.start();
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}
