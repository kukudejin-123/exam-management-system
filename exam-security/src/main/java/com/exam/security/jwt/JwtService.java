package com.exam.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 签发与解析。
 *
 * <p>只承担"编码/解码 + 过期判断"，不碰 Redis 与数据库——是否已登出由
 * {@link TokenBlacklist} 在调用侧判断。职责分开，单元测试不需要外部依赖。
 */
@Component
public class JwtService {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        byte[] raw = props.getSecret().getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            throw new IllegalStateException("JWT 密钥至少需要 32 字节，当前 " + raw.length + " 字节");
        }
        this.key = Keys.hmacShaKeyFor(raw);
    }

    /**
     * 签发 access token。
     *
     * @param userId  用户 id
     * @param role    角色名
     * @param classId 学生所属班级，教师/管理员传 null
     * @param jti     token 唯一标识，登出时按它加入黑名单
     */
    public String issueAccessToken(long userId, String role, Long classId, String jti) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .id(jti)
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuer(props.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.getAccessTtlMinutes(), ChronoUnit.MINUTES)));
        if (classId != null) {
            builder.claim("classId", classId);
        }
        return builder.signWith(key).compact();
    }

    /**
     * 签发 refresh token，有效期更长，仅用于换新的 access token。
     */
    public String issueRefreshToken(long userId, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(jti)
                .subject(String.valueOf(userId))
                .issuer(props.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.getRefreshTtlDays(), ChronoUnit.DAYS)))
                .signWith(key)
                .compact();
    }

    public String newJti() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 解析 token。
     *
     * @throws JwtException 签名无效、格式错误或已过期
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 解析失败（含过期）一律视为过期，由调用方兜底拒绝 */
    public boolean isExpired(String token) {
        try {
            return parse(token).getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    public long accessTtlSeconds() {
        return props.getAccessTtlMinutes() * 60;
    }
}
