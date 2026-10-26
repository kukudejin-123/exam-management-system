package com.exam.api.controller;

import com.exam.api.dto.FinalScoreResponse;
import com.exam.api.dto.GradeBookResponse;
import com.exam.api.dto.MyScoreResponse;
import com.exam.api.dto.ScoreUpdateRequest;
import com.exam.common.result.Result;
import com.exam.security.jwt.AuthPrincipal;
import com.exam.service.score.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * 成绩管理接口。
 *
 * <p>对应 F-09 特性族。修改成绩只有教师与管理员能调，且必须留痕。
 */
@Tag(name = "成绩管理")
@RestController
@RequestMapping("/api/scores")
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @Operation(summary = "班级成绩册（支持按分数/学号排序、按状态筛选）")
    @GetMapping("/exams/{examId}/grade-book")
    public Result<GradeBookResponse> gradeBook(
            @PathVariable long examId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String status) {
        return Result.ok(GradeBookResponse.from(scoreService.gradeBook(examId, sortBy, status)));
    }

    @Operation(summary = "班级总评（按考试权重加权，缺考不计入）")
    @GetMapping("/classes/{classId}/final")
    public Result<List<FinalScoreResponse>> finalScores(@PathVariable long classId) {
        return Result.ok(scoreService.finalScores(classId).stream()
                .map(FinalScoreResponse::from)
                .toList());
    }

    @Operation(summary = "修改成绩（必须留痕）")
    @PostMapping("/{scoreId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> update(@PathVariable long scoreId,
                               @Valid @RequestBody ScoreUpdateRequest request,
                               @AuthenticationPrincipal AuthPrincipal principal) {
        scoreService.updateScore(scoreId, request.finalScore(), principal.getUserId());
        return Result.ok();
    }

    @Operation(summary = "学生查自己的成绩")
    @GetMapping("/mine")
    public Result<List<MyScoreResponse>> mine(@AuthenticationPrincipal AuthPrincipal principal) {
        return Result.ok(scoreService.myScores(principal.getUserId(), principal.getUserId())
                .stream()
                .map(MyScoreResponse::from)
                .toList());
    }
}
