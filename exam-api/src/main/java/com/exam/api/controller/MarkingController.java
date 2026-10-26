package com.exam.api.controller;

import com.exam.api.dto.MarkingItemResponse;
import com.exam.api.dto.MarkingProgressResponse;
import com.exam.api.dto.MarkingRequest;
import com.exam.common.result.Result;
import com.exam.security.jwt.AuthPrincipal;
import com.exam.service.marking.MarkingProgress;
import com.exam.service.marking.MarkingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 主观题阅卷接口。
 *
 * <p>默认匿名评阅；实名复核需管理员显式传 revealNames=true。
 */
@Tag(name = "阅卷")
@RestController
@RequestMapping("/api/marking")
public class MarkingController {

    private final MarkingService markingService;

    public MarkingController(MarkingService markingService) {
        this.markingService = markingService;
    }

    @Operation(summary = "取某场考试某道简答题的阅卷队列（默认匿名）")
    @GetMapping("/queue")
    public Result<List<MarkingItemResponse>> queue(@RequestParam long examId,
                                                   @RequestParam long questionId,
                                                   @RequestParam(defaultValue = "false") boolean revealNames) {
        return Result.ok(markingService.queue(examId, questionId, revealNames).stream()
                .map(MarkingItemResponse::from)
                .toList());
    }

    @Operation(summary = "阅卷进度")
    @GetMapping("/progress")
    public Result<MarkingProgressResponse> progress(@RequestParam long examId,
                                                    @RequestParam long questionId) {
        MarkingProgress p = markingService.progress(examId, questionId);
        return Result.ok(new MarkingProgressResponse(p.total(), p.graded(), p.pending(), p.percent()));
    }

    @Operation(summary = "给一道简答题打分")
    @PostMapping("/grade")
    public Result<Void> grade(@Valid @RequestBody MarkingRequest request,
                              @AuthenticationPrincipal AuthPrincipal principal) {
        markingService.grade(request.attemptId(), request.questionId(),
                request.gained(), principal.getUserId());
        return Result.ok();
    }
}
