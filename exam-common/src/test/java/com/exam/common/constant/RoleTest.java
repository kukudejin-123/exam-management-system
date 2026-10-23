package com.exam.common.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("角色枚举")
class RoleTest {

    @Test
    @DisplayName("大小写不敏感解析")
    void from_ignores_case() {
        assertThat(Role.from("teacher")).isEqualTo(Role.TEACHER);
        assertThat(Role.from("ADMIN")).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("未知角色抛 IllegalArgumentException")
    void from_rejects_unknown() {
        assertThatThrownBy(() -> Role.from("boss"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown role");
    }

    @Test
    @DisplayName("四类角色齐全")
    void four_roles_exist() {
        assertThat(Role.values())
                .containsExactlyInAnyOrder(Role.ADMIN, Role.TEACHER, Role.MARKER, Role.STUDENT);
    }
}
