package com.exam.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtService 单元测试。
 *
 * <p>不连 Redis、不起 Spring 上下文——JwtService 只做编码解码，
 * 直接 new 出来测最省事，也说明这个类的职责划分是对的。
 */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-0123456789abcdef";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setAccessTtlMinutes(15);
        props.setRefreshTtlDays(7);
        props.setIssuer("exam-test");
        jwtService = new JwtService(props);
    }

    @Test
    @DisplayName("签发后能解析出 userId、role、classId 与签发方")
    void issue_then_parse() {
        String token = jwtService.issueAccessToken(42L, "STUDENT", 7L, jwtService.newJti());

        Claims claims = jwtService.parse(token);
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("role", String.class)).isEqualTo("STUDENT");
        assertThat(claims.get("classId", Long.class)).isEqualTo(7L);
        assertThat(claims.getIssuer()).isEqualTo("exam-test");
        assertThat(claims.getId()).isNotBlank();
    }

    @Test
    @DisplayName("classId 为 null 时不写入该 claim")
    void null_class_id_omitted() {
        String token = jwtService.issueAccessToken(42L, "TEACHER", null, jwtService.newJti());

        Claims claims = jwtService.parse(token);
        assertThat(claims.get("classId")).isNull();
        assertThat(claims.get("role", String.class)).isEqualTo("TEACHER");
    }

    @Test
    @DisplayName("同一用户两次签发得到不同 token（jti 不同）")
    void two_tokens_differ() {
        String a = jwtService.issueAccessToken(1L, "ADMIN", null, jwtService.newJti());
        String b = jwtService.issueAccessToken(1L, "ADMIN", null, jwtService.newJti());

        assertThat(a).isNotEqualTo(b);
        assertThat(jwtService.parse(a).getId()).isNotEqualTo(jwtService.parse(b).getId());
    }

    @Test
    @DisplayName("refresh token 有效期比 access token 长")
    void refresh_lives_longer() {
        String access = jwtService.issueAccessToken(1L, "ADMIN", null, jwtService.newJti());
        String refresh = jwtService.issueRefreshToken(1L, jwtService.newJti());

        assertThat(jwtService.parse(refresh).getExpiration())
                .isAfter(jwtService.parse(access).getExpiration());
    }

    @Test
    @DisplayName("过期 token 解析抛异常且 isExpired 为真")
    void expired_token_rejected() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setAccessTtlMinutes(-1); // 立即过期
        JwtService expiredService = new JwtService(props);

        String token = expiredService.issueAccessToken(1L, "ADMIN", null, expiredService.newJti());

        assertThat(expiredService.isExpired(token)).isTrue();
        assertThatThrownBy(() -> expiredService.parse(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("被篡改的 token 解析失败")
    void tampered_token_rejected() {
        String token = jwtService.issueAccessToken(1L, "ADMIN", null, jwtService.newJti());
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> jwtService.parse(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("密钥太短时构造即失败，避免上线用弱密钥")
    void short_secret_rejected() {
        JwtProperties props = new JwtProperties();
        props.setSecret("too-short");

        assertThatThrownBy(() -> new JwtService(props))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 字节");
    }

    @Test
    @DisplayName("accessTtlSeconds 按配置换算")
    void access_ttl_seconds() {
        assertThat(jwtService.accessTtlSeconds()).isEqualTo(15 * 60);
    }
}
