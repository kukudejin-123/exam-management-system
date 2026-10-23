package com.exam.api.dto;

import com.exam.repository.entity.SchoolClass;

/** 班级响应体 */
public record ClassResponse(Long id, String name, String subject, Long teacherId,
                            boolean archived) {

    public static ClassResponse from(SchoolClass c) {
        return new ClassResponse(c.getId(), c.getName(), c.getSubject(),
                c.getTeacherId(), c.getArchived() != null && c.getArchived() == 1);
    }
}
