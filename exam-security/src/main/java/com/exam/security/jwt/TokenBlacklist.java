package com.exam.security.jwt;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * token 黑名单：登出后让还没到期的 token 立即失效。
 *
 * <p>用 jti 而非整串 token 作 key，Redis 里存的是短标识；
 * TTL 设为 token 剩余有效期，到期自动清理，不需要额外任务。
 */
@Component
public class TokenBlacklist {

    private static final String PREFIX = "exam:token:blacklist:";

    private final StringRedisTemplate redis;

    public TokenBlacklist(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void revoke(String jti, long ttlSeconds) {
        if (jti == null || ttlSeconds <= 0) {
            return;
        }
        redis.opsForValue().set(PREFIX + jti, "1", Duration.of(ttlSeconds, ChronoUnit.SECONDS));
    }

    public boolean isRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
    }
}
