package com.exam.api.dto;

import com.exam.service.exam.ExamService;

/** 考试名单响应 */
public record ExamRosterResponse(long examId, int expected, int attended, int absent,
                                 java.util.List<Item> items) {

    public record Item(Long studentId, String studentName, Integer status) {

        public static Item from(ExamService.RosterItem item) {
            return new Item(item.studentId(), item.studentName(), item.status());
        }
    }
}
