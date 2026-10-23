package com.exam.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 作答记录：按 (exam_id, student_id, attempt_no) 唯一，保证判分幂等 */
@TableName("exam_attempt")
public class ExamAttempt extends BaseEntity {

    private Long examId;
    private Long studentId;
    private Integer attemptNo;
    /** 0 进行中 1 已交卷 2 到时自动交卷 */
    private Integer status;
    private LocalDateTime startedAt;
    private LocalDateTime submitAt;
    private Integer score;
    /** 行为留痕 JSON */
    private String cheatEvents;

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Integer getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(Integer attemptNo) {
        this.attemptNo = attemptNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getSubmitAt() {
        return submitAt;
    }

    public void setSubmitAt(LocalDateTime submitAt) {
        this.submitAt = submitAt;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getCheatEvents() {
        return cheatEvents;
    }

    public void setCheatEvents(String cheatEvents) {
        this.cheatEvents = cheatEvents;
    }
}
