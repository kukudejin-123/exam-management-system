package com.exam.api.dto;

import com.exam.service.marking.MarkingItem;

/** 阅卷队列条目响应 */
public record MarkingItemResponse(long attemptId, Long studentId, String studentName,
                                  String answer, Integer gained, boolean graded) {

    public static MarkingItemResponse from(MarkingItem item) {
        return new MarkingItemResponse(item.attemptId(), item.studentId(),
                item.studentName(), item.answer(), item.gained(), item.graded());
    }
}
