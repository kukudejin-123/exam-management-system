package com.exam.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/** 题目：独立于试卷存在，停用后不进组卷候选池但不影响历史试卷 */
@TableName("question")
public class Question extends BaseEntity {

    /** 题型 */
    public enum Type {
        SINGLE, MULTI, JUDGE, FILL, SHORT
    }

    private Long bankId;
    private String type;
    private String stem;
    /** 选项 JSON，如 {"A":"..","B":".."} */
    private String options;
    /** 答案 JSON，如 {"correct":["A"]} 或 {"accept":[".."]} */
    private String answer;
    /** 1 易 2 中 3 难 */
    private Integer difficulty;
    private String chapter;
    private String knowledge;
    private Integer status;

    public Long getBankId() {
        return bankId;
    }

    public void setBankId(Long bankId) {
        this.bankId = bankId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStem() {
        return stem;
    }

    public void setStem(String stem) {
        this.stem = stem;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public String getChapter() {
        return chapter;
    }

    public void setChapter(String chapter) {
        this.chapter = chapter;
    }

    public String getKnowledge() {
        return knowledge;
    }

    public void setKnowledge(String knowledge) {
        this.knowledge = knowledge;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
