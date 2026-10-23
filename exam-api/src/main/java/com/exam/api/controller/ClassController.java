package com.exam.api.controller;

import com.exam.api.dto.ClassRequest;
import com.exam.api.dto.ClassResponse;
import com.exam.api.dto.PageResponse;
import com.exam.common.page.PageQuery;
import com.exam.common.page.PageResult;
import com.exam.common.result.Result;
import com.exam.repository.entity.SchoolClass;
import com.exam.service.clazz.ClassService;
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
 * 班级管理接口，同时作为 CRUD 模板的样板。
 *
 * <p>权限：增删改只有管理员与教师能调，查询所有登录角色可调。
 * 教师的数据范围（只看自己的班级）在后续迭代里用 @PreAuthorize 加条件，
 * 第一版先保证接口级权限正确。
 */
@Tag(name = "班级管理")
@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @Operation(summary = "分页查询班级")
    @GetMapping
    public Result<PageResponse<ClassResponse>> page(
            @RequestParam(defaultValue = "1") @Min(1) long pageNo,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) String keyword) {
        PageQuery query = new PageQuery();
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        PageResult<SchoolClass> page = classService.page(query, keyword);
        return Result.ok(PageResponse.from(page, ClassResponse::from));
    }

    @Operation(summary = "查询全部班级（下拉选项用）")
    @GetMapping("/all")
    public Result<List<ClassResponse>> all() {
        return Result.ok(classService.listAll().stream().map(ClassResponse::from).toList());
    }

    @Operation(summary = "查询班级详情")
    @GetMapping("/{id}")
    public Result<ClassResponse> get(@PathVariable long id) {
        return Result.ok(ClassResponse.from(classService.getById(id)));
    }

    @Operation(summary = "创建班级")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Long> create(@Valid @RequestBody ClassRequest request) {
        return Result.ok(classService.create(request.name(), request.subject(), request.teacherId()));
    }

    @Operation(summary = "更新班级")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> update(@PathVariable long id, @Valid @RequestBody ClassRequest request) {
        classService.update(id, request.name(), request.subject(), request.teacherId());
        return Result.ok();
    }

    @Operation(summary = "归档/取消归档班级")
    @PutMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> archive(@PathVariable long id, @RequestParam boolean archived) {
        classService.archive(id, archived);
        return Result.ok();
    }

    @Operation(summary = "删除班级（软删除，历史成绩保留）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable long id) {
        classService.delete(id);
        return Result.ok();
    }
}
