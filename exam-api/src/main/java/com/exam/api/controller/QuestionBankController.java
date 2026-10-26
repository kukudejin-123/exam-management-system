package com.exam.api.controller;

import com.exam.common.result.Result;
import com.exam.repository.entity.QuestionBank;
import com.exam.service.question.QuestionBankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 题库下拉选项 */
@Tag(name = "题库")
@RestController
@RequestMapping("/api/question-banks")
public class QuestionBankController {

    private final QuestionBankService bankService;

    public QuestionBankController(QuestionBankService bankService) {
        this.bankService = bankService;
    }

    @Operation(summary = "全部题库（下拉选项用）")
    @GetMapping
    public Result<List<QuestionBank>> all() {
        return Result.ok(bankService.listAll());
    }
}
