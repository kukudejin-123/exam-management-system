package com.exam.security.jwt;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 当前登录用户的身份载体，从 JWT 解析出来后塞进 SecurityContext。
 *
 * <p>带上 classId 是因为教师只能看自己的班级——数据级隔离需要它，
 * 而 Security 默认的 UsernamePasswordAuthenticationToken 装不下业务字段。
 */
public final class AuthPrincipal implements Principal, UserDetails {

    private final long userId;
    private final String role;
    private final Long classId;

    public AuthPrincipal(long userId, String role, Long classId) {
        this.userId = userId;
        this.role = role;
        this.classId = classId;
    }

    /** 角色权限，供 Spring Security 的 hasRole 表达式使用 */
    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return String.valueOf(userId);
    }

    /** 账号永不过期：过期与否由 JWT 自身的 exp 决定 */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
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
