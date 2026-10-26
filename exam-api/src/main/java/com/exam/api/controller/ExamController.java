package com.exam.api.controller;

import com.exam.api.dto.ExamRequest;
import com.exam.api.dto.ExamRosterResponse;
import com.exam.common.result.Result;
import com.exam.repository.entity.Exam;
import com.exam.repository.entity.User;
import com.exam.service.exam.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 考试发布与考场管理接口。
 *
 * <p>对应 F-05 特性族。
 */
@Tag(name = "考试发布")
@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @Operation(summary = "创建并发布考试")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Long> create(@Valid @RequestBody ExamRequest request) {
        Exam exam = new Exam();
        exam.setPaperId(request.paperId());
        exam.setClassId(request.classId());
        exam.setName(request.name());
        exam.setStartAt(request.startAt());
        exam.setEndAt(request.endAt());
        exam.setDuration(request.duration());
        exam.setPassLine(request.passLine());
        exam.setMaxAttempts(request.maxAttempts() == null ? 1 : request.maxAttempts());
        exam.setScoreRule(request.scoreRule() == null ? "LAST" : request.scoreRule());
        exam.setPublishMode(request.publishMode() == null ? "IMMEDIATE" : request.publishMode());
        exam.setRetakePolicy(request.retakePolicy() == null ? 0 : request.retakePolicy());
        exam.setCreatedBy(request.createdBy());
        return Result.ok(examService.create(exam));
    }

    @Operation(summary = "考试详情")
    @GetMapping("/{id}")
    public Result<Exam> get(@PathVariable long id) {
        return Result.ok(examService.getById(id));
    }

    @Operation(summary = "某班级的考试列表")
    @GetMapping("/by-class/{classId}")
    public Result<List<Exam>> byClass(@PathVariable long classId) {
        return Result.ok(examService.listOfClass(classId));
    }

    @Operation(summary = "应考/实考/缺考名单")
    @GetMapping("/{id}/roster")
    public Result<ExamRosterResponse> roster(@PathVariable long id) {
        ExamService.ExamRoster r = examService.roster(id);
        List<ExamRosterResponse.Item> items = r.expectedList().stream()
                .map(ExamRosterResponse.Item::from)
                .toList();
        return Result.ok(new ExamRosterResponse(r.examId(), r.expected(), r.attended(),
                r.absent(), items));
    }

    @Operation(summary = "班级学生列表（发布前确认应考人数）")
    @GetMapping("/students/{classId}")
    public Result<List<User>> students(@PathVariable long classId) {
        return Result.ok(examService.studentsOf(classId));
    }
}
