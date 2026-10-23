package com.exam.service.question;

import com.exam.common.constant.ErrorCode;
import com.exam.common.exception.BizException;
import com.exam.common.page.PageQuery;
import com.exam.common.page.PageResult;
import com.exam.repository.entity.Question;
import com.exam.repository.mapper.QuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 题库与题目管理。
 *
 * <p>对应"数据的增删改查"预研要求中较复杂的一个：题目有标签筛选、
 * 停用后不进候选池但不影响历史试卷两个特殊规则。
 */
@Service
public class QuestionService {

    private static final Set<String> VALID_TYPES =
            Set.of("SINGLE", "MULTI", "JUDGE", "FILL", "SHORT");

    private final QuestionMapper questionMapper;

    public QuestionService(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    /** 分页 + 多标签组合筛选 */
    public PageResult<Question> page(PageQuery query, QuestionQuery filter) {
        Page<Question> page = Page.of(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(filter.bankId() != null, Question::getBankId, filter.bankId())
                .eq(hasText(filter.type()), Question::getType, filter.type())
                .eq(filter.difficulty() != null, Question::getDifficulty, filter.difficulty())
                .eq(hasText(filter.chapter()), Question::getChapter, filter.chapter())
                .like(hasText(filter.knowledge()), Question::getKnowledge, filter.knowledge())
                .eq(filter.status() != null, Question::getStatus, filter.status())
                .like(hasText(filter.keyword()), Question::getStem, filter.keyword())
                .orderByDesc(Question::getCreatedAt);

        Page<Question> result = questionMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    public Question getById(long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw BizException.notFound("题目");
        }
        return question;
    }

    @Transactional
    public long create(Question input) {
        validate(input);
        input.setStatus(input.getStatus() == null ? 1 : input.getStatus());
        questionMapper.insert(input);
        return input.getId();
    }

    /** 题库 Excel 导入的批量入口 */
    @Transactional
    public int createBatch(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return 0;
        }
        questions.forEach(this::validate);
        return questionMapper.insertBatch(questions);
    }

    @Transactional
    public void update(long id, Question input) {
        Question existing = getById(id);
        validate(input);
        existing.setType(input.getType());
        existing.setStem(input.getStem());
        existing.setOptions(input.getOptions());
        existing.setAnswer(input.getAnswer());
        existing.setDifficulty(input.getDifficulty());
        existing.setChapter(input.getChapter());
        existing.setKnowledge(input.getKnowledge());
        existing.setBankId(input.getBankId());
        questionMapper.updateById(existing);
    }

    /**
     * 停用题目。停用后不进组卷候选池，但已发布的试卷仍引用它——
     * 这正是题目与试卷必须分开建模的原因。
     */
    @Transactional
    public void changeStatus(long id, int status) {
        if (status != 0 && status != 1) {
            throw BizException.param("status 只能是 0（停用）或 1（启用）");
        }
        Question existing = getById(id);
        existing.setStatus(status);
        questionMapper.updateById(existing);
    }

    @Transactional
    public void delete(long id) {
        getById(id);
        questionMapper.deleteById(id);
    }

    private void validate(Question q) {
        if (q.getBankId() == null) {
            throw BizException.param("必须指定所属题库");
        }
        if (!hasText(q.getType()) || !VALID_TYPES.contains(q.getType())) {
            throw BizException.param("题型必须是 " + VALID_TYPES + " 之一");
        }
        if (!hasText(q.getStem())) {
            throw BizException.param("题干不能为空");
        }
        if (!hasText(q.getAnswer())) {
            throw BizException.param("答案不能为空");
        }
        if (q.getDifficulty() == null || q.getDifficulty() < 1 || q.getDifficulty() > 3) {
            throw BizException.param("难度必须是 1（易）/ 2（中）/ 3（难）");
        }
    }

    /** 筛选条件载体 */
    public record QuestionQuery(Long bankId, String type, Integer difficulty,
                                String chapter, String knowledge,
                                Integer status, String keyword) {
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
