package com.exam.service.paper;

import com.exam.common.exception.BizException;
import com.exam.repository.entity.Question;
import com.exam.repository.mapper.QuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 组卷引擎单元测试。
 *
 * <p>组卷错一次，整场考试的题目就错了——这是必须重点覆盖的模块。
 * 重点验证：配额正确、不重复抽题、总分严格相等、题量不足时阻断。
 */
@ExtendWith(MockitoExtension.class)
class PaperComposerTest {

    @Mock
    private QuestionMapper questionMapper;

    private PaperComposer composer;

    @BeforeEach
    void setUp() {
        composer = new PaperComposer(questionMapper);
    }

    /** 造一批题目：两章 × 三种题型 × 三个难度，共 18 题 */
    private List<Question> buildPool() {
        List<Question> pool = new ArrayList<>();
        int id = 1;
        for (String chapter : List.of("第一章", "第二章")) {
            for (String type : List.of("SINGLE", "JUDGE", "MULTI")) {
                for (int diff = 1; diff <= 3; diff++) {
                    Question q = new Question();
                    q.setId((long) id++);
                    q.setBankId(1L);
                    q.setType(type);
                    q.setChapter(chapter);
                    q.setDifficulty(diff);
                    q.setStem(chapter + "-" + type + "-" + diff);
                    q.setStatus(1);
                    pool.add(q);
                }
            }
        }
        return pool;
    }

    private void givenPool(List<Question> pool) {
        when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(pool);
    }

    @Test
    @DisplayName("按章节配额抽题，每章数量符合要求")
    void chapter_quota() {
        givenPool(buildPool());
        PaperSpec spec = new PaperSpec(
                List.of(new PaperSpec.ChapterQuota("第一章", 4),
                        new PaperSpec.ChapterQuota("第二章", 3)),
                null, null, 70);

        List<PaperComposer.ScoredQuestion> picked = composer.compose(List.of(1L), spec, false);

        assertThat(picked).hasSize(7);
        assertThat(picked.stream().filter(q -> "第一章".equals(q.question().getChapter())).count())
                .isEqualTo(4);
        assertThat(picked.stream().filter(q -> "第二章".equals(q.question().getChapter())).count())
                .isEqualTo(3);
    }

    @Test
    @DisplayName("总分严格等于目标总分，余数摊到前几题")
    void total_score_exact_with_remainder() {
        givenPool(buildPool());
        // 7 题分 100 分：100 / 7 = 14 余 2 → 前两题 15 分，其余 14 分
        PaperSpec spec = new PaperSpec(
                List.of(new PaperSpec.ChapterQuota("第一章", 4),
                        new PaperSpec.ChapterQuota("第二章", 3)),
                null, null, 100);

        List<PaperComposer.ScoredQuestion> picked = composer.compose(List.of(1L), spec, false);

        assertThat(picked.stream().mapToInt(PaperComposer.ScoredQuestion::score).sum()).isEqualTo(100);
        assertThat(picked.get(0).score()).isEqualTo(15);
        assertThat(picked.get(1).score()).isEqualTo(15);
        assertThat(picked.get(2).score()).isEqualTo(14);
    }

    @Test
    @DisplayName("总分整除时平均分配")
    void total_score_exact_division() {
        givenPool(buildPool());
        PaperSpec spec = new PaperSpec(
                List.of(new PaperSpec.ChapterQuota("第一章", 5)), null, null, 50);

        List<PaperComposer.ScoredQuestion> picked = composer.compose(List.of(1L), spec, false);

        assertThat(picked).hasSize(5);
        assertThat(picked).allMatch(q -> q.score() == 10);
    }

    @Test
    @DisplayName("同一道题不会被重复抽中")
    void no_duplicate_questions() {
        givenPool(buildPool());
        PaperSpec spec = new PaperSpec(
                List.of(new PaperSpec.ChapterQuota("第一章", 4),
                        new PaperSpec.ChapterQuota("第二章", 4)),
                null, List.of("SINGLE", "JUDGE"), 100);

        List<PaperComposer.ScoredQuestion> picked = composer.compose(List.of(1L), spec, false);

        List<Long> ids = picked.stream().map(q -> q.question().getId()).toList();
        assertThat(ids).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("按题型配额抽题")
    void type_quota() {
        givenPool(buildPool());
        PaperSpec spec = new PaperSpec(null, null, List.of("SINGLE", "JUDGE", "MULTI"), 30);

        List<PaperComposer.ScoredQuestion> picked = composer.compose(List.of(1L), spec, false);

        assertThat(picked).hasSize(3);
        assertThat(picked).extracting(q -> q.question().getType())
                .containsExactlyInAnyOrder("SINGLE", "JUDGE", "MULTI");
    }

    @Test
    @DisplayName("按难度配额抽题")
    void difficulty_quota() {
        givenPool(buildPool());
        PaperSpec spec = new PaperSpec(null, List.of(1, 2, 2), null, 50);

        List<PaperComposer.ScoredQuestion> picked = composer.compose(List.of(1L), spec, false);

        assertThat(picked).hasSize(3);
        assertThat(picked.stream().filter(q -> q.question().getDifficulty() == 1).count()).isEqualTo(1);
        assertThat(picked.stream().filter(q -> q.question().getDifficulty() == 2).count()).isEqualTo(2);
    }

    @Test
    @DisplayName("章节配额超过候选数量时阻断，并提示缺口")
    void insufficient_chapter_blocks() {
        givenPool(buildPool());
        // 第一章只有 9 题，要 20 题
        PaperSpec spec = new PaperSpec(
                List.of(new PaperSpec.ChapterQuota("第一章", 20)), null, null, 100);

        assertThatThrownBy(() -> composer.compose(List.of(1L), spec, false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("候选题目不足")
                .hasMessageContaining("第一章");
    }

    @Test
    @DisplayName("题库没有该题型时阻断")
    void insufficient_type_blocks() {
        givenPool(buildPool());
        PaperSpec spec = new PaperSpec(null, null, List.of("FILL"), 20);

        assertThatThrownBy(() -> composer.compose(List.of(1L), spec, false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("题型 FILL");
    }

    @Test
    @DisplayName("难度配额无候选时阻断")
    void insufficient_difficulty_blocks() {
        givenPool(buildPool());
        PaperSpec spec = new PaperSpec(null, List.of(9), null, 20);

        assertThatThrownBy(() -> composer.compose(List.of(1L), spec, false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("难度 9");
    }

    @Test
    @DisplayName("规格里三个维度都为空时拒绝组卷")
    void empty_spec_rejected() {
        // 不打桩：compose 在校验阶段就抛异常，走不到查库
        PaperSpec spec = new PaperSpec(null, null, null, 100);

        assertThatThrownBy(() -> composer.compose(List.of(1L), spec, false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("总题量必须大于 0");
    }

    @Test
    @DisplayName("未指定题库时拒绝组卷")
    void no_bank_rejected() {
        PaperSpec spec = new PaperSpec(
                List.of(new PaperSpec.ChapterQuota("第一章", 2)), null, null, 20);

        assertThatThrownBy(() -> composer.compose(List.of(), spec, false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("至少一个题库");
    }

    @Test
    @DisplayName("shuffle=true 时多次抽题会出现不同顺序")
    void shuffle_changes_order() {
        givenPool(buildPool());
        PaperSpec spec = new PaperSpec(
                List.of(new PaperSpec.ChapterQuota("第一章", 8)), null, null, 80);

        List<Integer> firstOrder = null;
        boolean changed = false;
        for (int i = 0; i < 30 && !changed; i++) {
            List<Integer> order = composer.compose(List.of(1L), spec, true).stream()
                    .map(q -> q.question().getId().intValue()).toList();
            if (firstOrder == null) {
                firstOrder = order;
            } else if (!firstOrder.equals(order)) {
                changed = true;
            }
        }
        assertThat(changed).isTrue();
    }

    @Test
    @DisplayName("shuffle=false 时按章节配额顺序输出，不打乱")
    void no_shuffle_keeps_quota_order() {
        givenPool(buildPool());
        PaperSpec spec = new PaperSpec(
                List.of(new PaperSpec.ChapterQuota("第二章", 2),
                        new PaperSpec.ChapterQuota("第一章", 2)),
                null, null, 40);

        List<String> chapters = composer.compose(List.of(1L), spec, false).stream()
                .map(q -> q.question().getChapter()).toList();

        // 配额里第二章在前，输出就必须前两题是第二章、后两题是第一章
        assertThat(chapters).containsExactly("第二章", "第二章", "第一章", "第一章");
    }

    @Test
    @DisplayName("shuffle=false 时同一章节内仍是随机选题，不会每次都取前几题")
    void no_shuffle_still_random_within_chapter() {
        givenPool(buildPool());
        PaperSpec spec = new PaperSpec(
                List.of(new PaperSpec.ChapterQuota("第一章", 3)), null, null, 30);

        // 第一章共 9 题抽 3 题，跑多次应出现不同组合
        List<Long> first = null;
        boolean changed = false;
        for (int i = 0; i < 30 && !changed; i++) {
            List<Long> ids = composer.compose(List.of(1L), spec, false).stream()
                    .map(q -> q.question().getId()).toList();
            if (first == null) {
                first = ids;
            } else if (!first.equals(ids)) {
                changed = true;
            }
        }
        assertThat(changed).isTrue();
    }

    @Test
    @DisplayName("停用的题目不进候选池")
    void disabled_questions_excluded() {
        List<Question> pool = buildPool();
        // 把第一章的题全部停用
        pool.stream().filter(q -> "第一章".equals(q.getChapter())).forEach(q -> q.setStatus(0));
        givenPool(pool);
        PaperSpec spec = new PaperSpec(
                List.of(new PaperSpec.ChapterQuota("第一章", 2)), null, null, 20);

        assertThatThrownBy(() -> composer.compose(List.of(1L), spec, false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("候选题目不足");
    }

    @Test
    @DisplayName("大题库下抽题仍不重复且总分正确")
    void large_pool_no_duplicate() {
        List<Question> pool = new ArrayList<>();
        IntStream.rangeClosed(1, 500).forEach(i -> {
            Question q = new Question();
            q.setId((long) i);
            q.setBankId(1L);
            q.setType("SINGLE");
            q.setChapter("第" + (i % 10 + 1) + "章");
            q.setDifficulty(i % 3 + 1);
            q.setStem("题" + i);
            q.setStatus(1);
            pool.add(q);
        });
        givenPool(pool);
        PaperSpec spec = new PaperSpec(
                List.of(new PaperSpec.ChapterQuota("第1章", 30),
                        new PaperSpec.ChapterQuota("第5章", 30)),
                null, null, 600);

        List<PaperComposer.ScoredQuestion> picked = composer.compose(List.of(1L), spec, false);

        assertThat(picked).hasSize(60);
        assertThat(picked.stream().map(q -> q.question().getId()).toList()).doesNotHaveDuplicates();
        assertThat(picked.stream().mapToInt(PaperComposer.ScoredQuestion::score).sum()).isEqualTo(600);
    }
}
