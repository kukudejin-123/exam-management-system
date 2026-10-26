package com.exam.service.paper;

import com.exam.common.constant.ErrorCode;
import com.exam.common.exception.BizException;
import com.exam.repository.entity.Question;
import com.exam.repository.mapper.QuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 组卷引擎：按规格从题库抽题。
 *
 * <p>设计原则（来自竞品分析结论与立项评审问题 1）：
 * 第一版只做"选题方式枚举"，不做通用约束求解。
 * 五个维度——随机、章节、顺序、题型、难度——中，可量化的三个（章节、题型、难度）
 * 在这里落实；随机与顺序由调用方控制 shuffle 开关。
 *
 * <p>**关键约束**：候选题目不足时抛出 INSUFFICIENT_QUESTIONS 并带上缺口数量，
 * 绝不静默降级——静默降级会让老师以为卷子出对了，实际少了题。
 */
@Component
public class PaperComposer {

    private final QuestionMapper questionMapper;
    private final Random random = new Random();

    public PaperComposer(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    /**
     * 按规格抽题。
     *
     * @param bankIds   参与抽题的题库（可为多个）
     * @param spec      组卷规格
     * @param shuffle   是否打乱题目顺序；false 时按章节配额顺序输出
     * @return 抽中的题目，带本题分值（平均分配）
     * @throws BizException 候选不足时抛出，message 含缺口数量
     */
    public List<ScoredQuestion> compose(List<Long> bankIds, PaperSpec spec, boolean shuffle) {
        if (bankIds == null || bankIds.isEmpty()) {
            throw BizException.param("组卷必须指定至少一个题库");
        }
        if (spec.totalQuestions() <= 0) {
            throw BizException.param("组卷规格的总题量必须大于 0");
        }

        // 一次性把候选题目全部取出，再在内存里按配额挑。
        // 题库规模在万级以内时这样比反复查库快，也便于做"不重复"约束。
        //
        // 这里再按 status 过滤一次：SQL 条件里已经过滤，但组件被别处复用
        // （或查询条件被改动）时，这道防线能保证停用题目绝不会进卷。
        List<Question> pool = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .in(Question::getBankId, bankIds)
                        .eq(Question::getStatus, 1))
                .stream()
                .filter(q -> q.getStatus() != null && q.getStatus() == 1)
                .toList();

        List<Question> picked = pickByQuota(pool, spec);
        if (shuffle) {
            Collections.shuffle(picked, random);
        }

        int perQuestion = spec.totalScore() / picked.size();
        int remainder = spec.totalScore() % picked.size();
        List<ScoredQuestion> result = new ArrayList<>(picked.size());
        for (int i = 0; i < picked.size(); i++) {
            // 余数摊到前几题，保证总分严格等于目标总分
            int score = perQuestion + (i < remainder ? 1 : 0);
            result.add(new ScoredQuestion(picked.get(i), score));
        }
        return result;
    }

    /**
     * 按配额挑题：先满足章节配额，再用题型/难度配额补齐。
     *
     * <p>同一道题不会被重复选中——用 id 去重，这是"同场不同卷"的基础。
     */
    private List<Question> pickByQuota(List<Question> pool, PaperSpec spec) {
        List<Question> chosen = new ArrayList<>();
        List<Long> usedIds = new ArrayList<>();

        // 1) 按章节配额抽
        if (spec.chapters() != null) {
            for (PaperSpec.ChapterQuota quota : spec.chapters()) {
                List<Question> inChapter = pool.stream()
                        .filter(q -> quota.chapter().equals(q.getChapter()))
                        .filter(q -> !usedIds.contains(q.getId()))
                        .collect(Collectors.toCollection(ArrayList::new));
                if (inChapter.size() < quota.count()) {
                    throw insufficient(quota.chapter(), quota.count(), inChapter.size());
                }
                Collections.shuffle(inChapter, random);
                for (int i = 0; i < quota.count(); i++) {
                    chosen.add(inChapter.get(i));
                    usedIds.add(inChapter.get(i).getId());
                }
            }
        }

        // 2) 按题型配额抽
        if (spec.types() != null) {
            for (String type : spec.types()) {
                List<Question> ofType = pool.stream()
                        .filter(q -> type.equals(q.getType()))
                        .filter(q -> !usedIds.contains(q.getId()))
                        .collect(Collectors.toCollection(ArrayList::new));
                if (ofType.isEmpty()) {
                    throw insufficient("题型 " + type, 1, 0);
                }
                Collections.shuffle(ofType, random);
                chosen.add(ofType.get(0));
                usedIds.add(ofType.get(0).getId());
            }
        }

        // 3) 按难度配额抽
        if (spec.difficulty() != null) {
            for (Integer level : spec.difficulty()) {
                List<Question> ofLevel = pool.stream()
                        .filter(q -> level.equals(q.getDifficulty()))
                        .filter(q -> !usedIds.contains(q.getId()))
                        .collect(Collectors.toCollection(ArrayList::new));
                if (ofLevel.isEmpty()) {
                    throw insufficient("难度 " + level, 1, 0);
                }
                Collections.shuffle(ofLevel, random);
                chosen.add(ofLevel.get(0));
                usedIds.add(ofLevel.get(0).getId());
            }
        }

        if (chosen.isEmpty()) {
            throw BizException.param("组卷规格必须至少指定章节、题型或难度配额之一");
        }
        return chosen;
    }

    private BizException insufficient(String dim, int need, int have) {
        return new BizException(ErrorCode.INSUFFICIENT_QUESTIONS,
                String.format("候选题目不足：%s 需要 %d 题，可用 %d 题。请补充题库或调整配比。",
                        dim, need, have));
    }

    /** 抽中的一道题及其在卷上的分值 */
    public record ScoredQuestion(Question question, int score) {
    }

    /** 候选池概况，供组卷前预检 */
    public record PoolStat(long total, Map<String, Long> byType, Map<Integer, Long> byDifficulty,
                           Map<String, Long> byChapter) {
    }
}
