package com.exam.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/** 系统用户：管理员 / 教师 / 阅卷人 / 学生 */
@TableName("sys_user")
public class User extends BaseEntity {

    private String userNo;
    private String name;
    private String passwordHash;
    private String role;
    private Long classId;
    private Integer status;

    public String getUserNo() {
        return userNo;
    }

    public void setUserNo(String userNo) {
        this.userNo = userNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
