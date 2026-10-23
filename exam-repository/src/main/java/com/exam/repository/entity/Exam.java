package com.exam.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 考试：一场考试的发布与考场参数 */
@TableName("exam")
public class Exam extends BaseEntity {

    /** 成绩取值规则 */
    public enum ScoreRule {
        HIGHEST, LAST, AVERAGE
    }

    /** 成绩发布模式 */
    public enum PublishMode {
        IMMEDIATE, DELAYED, MANUAL
    }

    private Long paperId;
    private Long classId;
    private String name;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    /** 答题时长（分钟） */
    private Integer duration;
    private Integer passLine;
    private Integer maxAttempts;
    private String scoreRule;
    /** 缺考是否允许补考：0 否 1 是 */
    private Integer retakePolicy;
    private String publishMode;
    /** 0 未开始 1 进行中 2 已结束 */
    private Integer status;
    private Long createdBy;

    public Long getPaperId() {
        return paperId;
    }

    public void setPaperId(Long paperId) {
        this.paperId = paperId;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getPassLine() {
        return passLine;
    }

    public void setPassLine(Integer passLine) {
        this.passLine = passLine;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getScoreRule() {
        return scoreRule;
    }

    public void setScoreRule(String scoreRule) {
        this.scoreRule = scoreRule;
    }

    public Integer getRetakePolicy() {
        return retakePolicy;
    }

    public void setRetakePolicy(Integer retakePolicy) {
        this.retakePolicy = retakePolicy;
    }

    public String getPublishMode() {
        return publishMode;
    }

    public void setPublishMode(String publishMode) {
        this.publishMode = publishMode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
