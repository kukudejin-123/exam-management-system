package com.exam.api.controller;

import com.exam.api.dto.PageResponse;
import com.exam.api.dto.QuestionRequest;
import com.exam.api.dto.QuestionResponse;
import com.exam.common.page.PageQuery;
import com.exam.common.page.PageResult;
import com.exam.common.result.Result;
import com.exam.repository.entity.Question;
import com.exam.service.question.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 题库与题目接口。
 *
 * <p>权限：教师与管理员可写，阅卷人与学生只读（阅卷时需要看题，
 * 但学生只能在考试时看到分配给自己的卷子，不能翻题库）。
 */
@Tag(name = "题库管理")
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @Operation(summary = "分页查询题目")
    @GetMapping
    public Result<PageResponse<QuestionResponse>> page(
            @RequestParam(defaultValue = "1") @Min(1) long pageNo,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) Long bankId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String chapter,
            @RequestParam(required = false) String knowledge,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        PageQuery query = new PageQuery();
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        QuestionService.QuestionQuery filter = new QuestionService.QuestionQuery(
                bankId, type, difficulty, chapter, knowledge, status, keyword);
        PageResult<Question> page = questionService.page(query, filter);
        return Result.ok(PageResponse.from(page, QuestionResponse::from));
    }

    @Operation(summary = "查询题目详情")
    @GetMapping("/{id}")
    public Result<QuestionResponse> get(@PathVariable long id) {
        return Result.ok(QuestionResponse.from(questionService.getById(id)));
    }

    @Operation(summary = "创建题目")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Long> create(@Valid @RequestBody QuestionRequest request) {
        Question input = toEntity(request);
        return Result.ok(questionService.create(input));
    }

    @Operation(summary = "批量导入题目")
    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Integer> createBatch(@Valid @RequestBody List<@Valid QuestionRequest> requests) {
        List<Question> inputs = requests.stream().map(QuestionController::toEntity).toList();
        return Result.ok(questionService.createBatch(inputs));
    }

    @Operation(summary = "更新题目")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> update(@PathVariable long id, @Valid @RequestBody QuestionRequest request) {
        Question input = toEntity(request);
        questionService.update(id, input);
        return Result.ok();
    }

    @Operation(summary = "启用/停用题目（停用后不进组卷候选池）")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> changeStatus(@PathVariable long id, @RequestParam int status) {
        questionService.changeStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "删除题目")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> delete(@PathVariable long id) {
        questionService.delete(id);
        return Result.ok();
    }

    private static Question toEntity(QuestionRequest r) {
        Question q = new Question();
        q.setBankId(r.bankId());
        q.setType(r.type());
        q.setStem(r.stem());
        q.setOptions(r.options());
        q.setAnswer(r.answer());
        q.setDifficulty(r.difficulty());
        q.setChapter(r.chapter());
        q.setKnowledge(r.knowledge());
        q.setStatus(r.status() == null ? 1 : r.status());
        return q;
    }
}
