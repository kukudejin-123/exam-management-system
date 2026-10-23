package com.exam.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/** 试卷：手工组卷或按规则生成 */
@TableName("exam_paper")
public class ExamPaper extends BaseEntity {

    private String name;
    /** 来源组卷规则，NULL 表示手工组卷 */
    private Long ruleId;
    private Integer totalScore;
    private Integer questionCount;
    /** 1 草稿 2 已发布 */
    private Integer status;
    private Long createdBy;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
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
