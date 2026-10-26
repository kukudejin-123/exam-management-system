package com.exam.api.dto;

/** 阅卷进度响应 */
public record MarkingProgressResponse(int total, int graded, int pending, double percent) {
}
