package com.exam.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/** 题库：题目归属的容器，可跨试卷复用 */
@TableName("question_bank")
public class QuestionBank extends BaseEntity {

    private String name;
    private String subject;
    private Long ownerId;
    private Integer status;

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

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
