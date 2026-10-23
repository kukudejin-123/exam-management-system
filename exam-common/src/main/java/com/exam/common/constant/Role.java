package com.exam.common.constant;

/**
 * 四类角色。对应 wiki/10 页 US-111 的 RBAC 设计。
 */
public enum Role {

    /** 系统管理员 */
    ADMIN,
    /** 教师：只能看到自己带的班级 */
    TEACHER,
    /** 阅卷人：主观题评阅 */
    MARKER,
    /** 学生：答题与查分 */
    STUDENT;

    public static Role from(String value) {
        for (Role r : values()) {
            if (r.name().equalsIgnoreCase(value)) {
                return r;
            }
        }
        throw new IllegalArgumentException("unknown role: " + value);
    }
}
