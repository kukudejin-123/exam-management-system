package com.exam.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/** 成绩：独立于考试存在，可按班级汇总，缺考也占一行 */
@TableName("score")
public class Score extends BaseEntity {

    /** 成绩状态 */
    public enum Status {
        /** 正常 */
        NORMAL(1),
        /** 缺考 */
        ABSENT(2),
        /** 0 分 */
        ZERO(3),
        /** 未阅卷 */
        UNGRADED(4);

        private final int code;

        Status(int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }
    }

    private Long studentId;
    private Long classId;
    private Long examId;
    private Integer rawScore;
    private Integer finalScore;
    private Integer passed;
    /** 1 正常 2 缺考 3 0分 4 未阅卷 */
    private Integer status;
    private Long gradedBy;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public Integer getRawScore() {
        return rawScore;
    }

    public void setRawScore(Integer rawScore) {
        this.rawScore = rawScore;
    }

    public Integer getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(Integer finalScore) {
        this.finalScore = finalScore;
    }

    public Integer getPassed() {
        return passed;
    }

    public void setPassed(Integer passed) {
        this.passed = passed;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getGradedBy() {
        return gradedBy;
    }

    public void setGradedBy(Long gradedBy) {
        this.gradedBy = gradedBy;
    }
}
