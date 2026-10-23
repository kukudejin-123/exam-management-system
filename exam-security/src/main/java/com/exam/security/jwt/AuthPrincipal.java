package com.exam.security.jwt;

import java.security.Principal;
import java.util.Objects;

/**
 * 当前登录用户的身份载体，从 JWT 解析出来后塞进 SecurityContext。
 *
 * <p>带上 classId 是因为教师只能看自己的班级——数据级隔离需要它，
 * 而 Security 默认的 UsernamePasswordAuthenticationToken 装不下业务字段。
 */
public final class AuthPrincipal implements Principal {

    private final long userId;
    private final String role;
    private final Long classId;

    public AuthPrincipal(long userId, String role, Long classId) {
        this.userId = userId;
        this.role = role;
        this.classId = classId;
    }

    public long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    /** 学生所属班级；教师与管理员的 classId 为 null */
    public Long getClassId() {
        return classId;
    }

    public boolean hasRole(String roleName) {
        return Objects.equals(role, roleName);
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthPrincipal other)) {
            return false;
        }
        return userId == other.userId && Objects.equals(role, other.role)
                && Objects.equals(classId, other.classId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, role, classId);
    }

    @Override
    public String toString() {
        return "AuthPrincipal{userId=" + userId + ", role='" + role + "'}";
    }
}
