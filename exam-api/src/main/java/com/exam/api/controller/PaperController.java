package com.exam.api.controller;

import com.exam.api.dto.PageResponse;
import com.exam.api.dto.PaperQuestionResponse;
import com.exam.common.page.PageQuery;
import com.exam.common.page.PageResult;
import com.exam.common.result.Result;
import com.exam.repository.entity.ExamPaper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import com.exam.service.paper.PaperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 试卷与组卷接口。
 *
 * <p>对应 F-04 组卷特性族。预览与正式组卷走同一个引擎，
 * 保证教师预览看到的就是正式考试得到的。
 */
@Tag(name = "试卷与组卷")
@RestController
@RequestMapping("/api/papers")
public class PaperController {

    private final PaperService paperService;

    public PaperController(PaperService paperService) {
        this.paperService = paperService;
    }

    @Operation(summary = "分页查询试卷")
    @GetMapping
    public Result<PageResponse<ExamPaper>> page(
            @RequestParam(defaultValue = "1") @Min(1) long pageNo,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) String keyword) {
        PageQuery query = new PageQuery();
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        PageResult<ExamPaper> page = paperService.page(query, keyword);
        return Result.ok(PageResponse.from(page, p -> p));
    }

    @Operation(summary = "试卷详情")
    @GetMapping("/{id}")
    public Result<ExamPaper> get(@PathVariable long id) {
        return Result.ok(paperService.getById(id));
    }

    @Operation(summary = "试卷题目清单（含题号与分值）")
    @GetMapping("/{id}/questions")
    public Result<java.util.List<PaperQuestionResponse>> questions(@PathVariable long id) {
        return Result.ok(paperService.questionsOf(id).stream()
                .map(PaperQuestionResponse::from)
                .toList());
    }

    @Operation(summary = "按规则抽题生成试卷")
    @PostMapping("/compose")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Long> compose(@RequestParam long ruleId,
                                @RequestParam(defaultValue = "true") boolean shuffle) {
        return Result.ok(paperService.composeFromRule(ruleId, shuffle));
    }

    @Operation(summary = "直接按规格组卷（不落规则，供前端组卷器）")
    @PostMapping("/compose-inline")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Long> composeInline(@Valid @RequestBody com.exam.service.paper.ComposeRequest request) {
        return Result.ok(paperService.composeInline(request));
    }

    @Operation(summary = "按规格预览（不落库）")
    @PostMapping("/preview-inline")
    public Result<java.util.List<PaperQuestionResponse>> previewInline(
            @Valid @RequestBody com.exam.service.paper.ComposeRequest request) {
        return Result.ok(paperService.previewInline(request).stream()
                .map(sq -> PaperQuestionResponse.of(sq.question(), sq.score(), 0))
                .toList());
    }

    @Operation(summary = "预览组卷结果（不落库）")
    @GetMapping("/preview")
    public Result<java.util.List<PaperQuestionResponse>> preview(@RequestParam long ruleId,
                                                                 @RequestParam(defaultValue = "false") boolean shuffle) {
        return Result.ok(paperService.previewRule(ruleId, shuffle).stream()
                .map(sq -> PaperQuestionResponse.of(sq.question(), sq.score(), 0))
                .toList());
    }

    @Operation(summary = "分值校验")
    @GetMapping("/{id}/validate")
    public Result<Void> validate(@PathVariable long id) {
        paperService.validateScore(id);
        return Result.ok();
    }

    @Operation(summary = "发布试卷（分值不平整时阻断）")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> publish(@PathVariable long id) {
        paperService.publish(id);
        return Result.ok();
    }
}
