package com.exam.api.controller;

import com.exam.api.dto.AnswerRequest;
import com.exam.api.dto.AttemptPaperResponse;
import com.exam.api.dto.SubmitResponse;
import com.exam.common.result.Result;
import com.exam.repository.entity.ExamAttempt;
import com.exam.security.jwt.AuthPrincipal;
import com.exam.service.attempt.AttemptPaper;
import com.exam.service.attempt.AttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 在线答题接口。
 *
 * <p>对应 F-06 特性族。学生身份从 JWT 取，不从请求体取——
 * 否则任何人都能替别人交卷。
 */
@Tag(name = "在线答题")
@RestController
@RequestMapping("/api/attempts")
public class AttemptController {

    private final AttemptService attemptService;

    public AttemptController(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @Operation(summary = "进入考试（断网重连会复用同一次尝试）")
    @PostMapping("/exams/{examId}/enter")
    public Result<Long> enter(@PathVariable long examId,
                              @AuthenticationPrincipal AuthPrincipal principal) {
        ExamAttempt attempt = attemptService.enter(examId, principal.getUserId());
        return Result.ok(attempt.getId());
    }

    @Operation(summary = "取卷面与已保存作答（不含答案）")
    @GetMapping("/{attemptId}/paper")
    public Result<AttemptPaperResponse> paper(@PathVariable long attemptId) {
        return Result.ok(AttemptPaperResponse.from(attemptService.paper(attemptId)));
    }

    @Operation(summary = "保存一道题的作答（实时落库，不等交卷）")
    @PostMapping("/{attemptId}/answers")
    public Result<Void> saveAnswer(@PathVariable long attemptId,
                                   @Valid @RequestBody AnswerRequest request) {
        attemptService.saveAnswer(attemptId, request.questionId(), request.answer());
        return Result.ok();
    }

    @Operation(summary = "交卷（幂等，重复提交返回已有成绩）")
    @PostMapping("/{attemptId}/submit")
    public Result<SubmitResponse> submit(@PathVariable long attemptId) {
        return Result.ok(SubmitResponse.from(attemptService.submit(attemptId)));
    }

    @Operation(summary = "剩余秒数（服务端计算）")
    @GetMapping("/{attemptId}/remaining")
    public Result<Long> remaining(@PathVariable long attemptId) {
        AttemptPaper paper = attemptService.paper(attemptId);
        return Result.ok(paper.remainingSeconds());
    }
}
