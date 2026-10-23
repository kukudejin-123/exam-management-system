package com.exam.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/** 组卷规则：从题库抽题的规则，与试卷分开建模 */
@TableName("paper_rule")
public class PaperRule extends BaseEntity {

    private String name;
    /** 抽题规格 JSON */
    private String spec;
    private Integer totalScore;
    private Long createdBy;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
