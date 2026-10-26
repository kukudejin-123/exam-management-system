package com.exam.service.paper;

import com.exam.common.constant.ErrorCode;
import com.exam.common.exception.BizException;
import com.exam.common.page.PageQuery;
import com.exam.common.page.PageResult;
import com.exam.repository.entity.ExamPaper;
import com.exam.repository.entity.PaperRule;
import com.exam.repository.mapper.ExamPaperMapper;
import com.exam.repository.mapper.PaperRuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 试卷与组卷规则管理。
 *
 * <p>对应 F-04-1 手工组卷、F-04-2 配比抽题、F-04-4 预览与分值校验。
 */
@Service
public class PaperService {

    private final ExamPaperMapper paperMapper;
    private final PaperRuleMapper ruleMapper;
    private final PaperComposer composer;
    private final ObjectMapper json;

    public PaperService(ExamPaperMapper paperMapper,
                        PaperRuleMapper ruleMapper,
                        PaperComposer composer,
                        ObjectMapper json) {
        this.paperMapper = paperMapper;
        this.ruleMapper = ruleMapper;
        this.composer = composer;
        this.json = json;
    }

    // ---------- 组卷规则 ----------

    /**
     * 按规则自动抽题生成试卷。
     *
     * <p>这是"配比抽题"的入口：规则里的 spec JSON 描述章节/题型/难度配额，
     * 由 PaperComposer 执行抽题，结果落成试卷-题目关联。
     */
    @Transactional
    public long composeFromRule(long ruleId, boolean shuffle) {
        PaperRule rule = ruleMapper.selectById(ruleId);
        if (rule == null) {
            throw BizException.notFound("组卷规则");
        }
        List<Long> bankIds = bankIdsOfRule(ruleId);
        if (bankIds.isEmpty()) {
            throw BizException.param("组卷规则未关联任何题库，无法抽题");
        }

        PaperSpec spec = parseSpec(rule.getSpec());
        List<PaperComposer.ScoredQuestion> picked = composer.compose(bankIds, spec, shuffle);

        ExamPaper paper = new ExamPaper();
        paper.setName(rule.getName());
        paper.setRuleId(ruleId);
        paper.setStatus(1);
        paper.setCreatedBy(rule.getCreatedBy());
        paper.setTotalScore(sumScore(picked));
        paper.setQuestionCount(picked.size());
        paperMapper.insert(paper);

        for (int i = 0; i < picked.size(); i++) {
            PaperComposer.ScoredQuestion sq = picked.get(i);
            paperMapper.insertPaperQuestion(paper.getId(), sq.question().getId(), i + 1, sq.score());
        }
        return paper.getId();
    }

    /**
     * 预览：按规则抽题但不落库，供教师确认卷面。
     *
     * <p>预览与正式组卷走同一个 composer，保证"看到的就是得到的"。
     */
    public List<PaperComposer.ScoredQuestion> previewRule(long ruleId, boolean shuffle) {
        PaperRule rule = ruleMapper.selectById(ruleId);
        if (rule == null) {
            throw BizException.notFound("组卷规则");
        }
        List<Long> bankIds = bankIdsOfRule(ruleId);
        if (bankIds.isEmpty()) {
            throw BizException.param("组卷规则未关联任何题库，无法预览");
        }
        return composer.compose(bankIds, parseSpec(rule.getSpec()), shuffle);
    }

    // ---------- 试卷 ----------

    public PageResult<ExamPaper> page(PageQuery query, String keyword) {
        Page<ExamPaper> page = Page.of(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<ExamPaper>()
                .like(keyword != null && !keyword.isBlank(), ExamPaper::getName, keyword)
                .orderByDesc(ExamPaper::getCreatedAt);
        Page<ExamPaper> result = paperMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    public ExamPaper getById(long id) {
        ExamPaper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw BizException.notFound("试卷");
        }
        return paper;
    }

    /** 试卷上的题目清单（含题号与分值） */
    public List<PaperQuestionView> questionsOf(long paperId) {
        getById(paperId);
        return paperMapper.selectPaperQuestions(paperId).stream()
                .map(PaperQuestionView::from)
                .toList();
    }

    /**
     * 分值校验：试卷总分必须等于各题分值之和，否则不允许发布。
     * 对应 F-04-4"分值不平整时阻断发布"。
     */
    public void validateScore(long paperId) {
        ExamPaper paper = getById(paperId);
        int sum = paperMapper.sumQuestionScore(paperId);
        if (sum != paper.getTotalScore()) {
            throw new BizException(ErrorCode.STATE_CONFLICT,
                    String.format("试卷总分 %d 与各题分值之和 %d 不一致，请调整后再发布",
                            paper.getTotalScore(), sum));
        }
    }

    @Transactional
    public void publish(long paperId) {
        validateScore(paperId);
        ExamPaper paper = getById(paperId);
        if (paper.getQuestionCount() == null || paper.getQuestionCount() <= 0) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "试卷没有题目，不能发布");
        }
        paper.setStatus(2);
        paperMapper.updateById(paper);
    }

    // ---------- 内部 ----------

    private int sumScore(List<PaperComposer.ScoredQuestion> picked) {
        return picked.stream().mapToInt(PaperComposer.ScoredQuestion::score).sum();
    }

    private List<Long> bankIdsOfRule(long ruleId) {
        return ruleMapper.selectBankIds(ruleId);
    }

    private PaperSpec parseSpec(String raw) {
        try {
            Map<String, Object> m = json.readValue(raw, new TypeReference<Map<String, Object>>() {
            });
            List<PaperSpec.ChapterQuota> chapters = null;
            if (m.get("chapters") instanceof List<?> list) {
                chapters = list.stream()
                        .filter(o -> o instanceof Map)
                        .map(o -> (Map<?, ?>) o)
                        .map(cm -> new PaperSpec.ChapterQuota(
                                String.valueOf(cm.get("chapter")),
                                ((Number) cm.get("count")).intValue()))
                        .toList();
            }
            List<Integer> difficulty = intList(m.get("difficulty"));
            List<String> types = strList(m.get("types"));
            int totalScore = m.get("totalScore") instanceof Number n ? n.intValue() : 100;
            return new PaperSpec(chapters, difficulty, types, totalScore);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "组卷规格 JSON 解析失败: " + raw);
        }
    }

    private List<Integer> intList(Object o) {
        if (!(o instanceof List<?> list)) {
            return null;
        }
        return list.stream().map(x -> ((Number) x).intValue()).toList();
    }

    private List<String> strList(Object o) {
        if (!(o instanceof List<?> list)) {
            return null;
        }
        return list.stream().map(String::valueOf).toList();
    }

    /** 试卷题目视图 */
    public record PaperQuestionView(Long questionId, int seq, int score, String type,
                                    String stem, String options, String answer) {

        static PaperQuestionView from(com.exam.repository.mapper.ExamPaperMapper.PaperQuestionRow row) {
            return new PaperQuestionView(row.questionId(), row.seq(), row.score(),
                    row.type(), row.stem(), row.options(), row.answer());
        }
    }
}
