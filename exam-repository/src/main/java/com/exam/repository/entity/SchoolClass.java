package com.exam.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/** 班级：组织单元，只做班级一层 */
@TableName("sys_class")
public class SchoolClass extends BaseEntity {

    private String name;
    private String subject;
    private Long teacherId;
    private Integer archived;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public Integer getArchived() {
        return archived;
    }

    public void setArchived(Integer archived) {
        this.archived = archived;
    }
}
