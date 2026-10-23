package com.exam.service.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * 登录失败计数与账号锁定。
 *
 * <p>Redis 里存两个键：失败次数（TTL = 锁定时长，自然清零）与锁定标记。
 * 抽成独立组件是为了让 AuthService 的单测能替换掉 Redis——
 * 登录逻辑的测试不该依赖外部中间件。
 */
@Component
public class LoginAttemptGuard {

    private static final String FAIL_PREFIX = "exam:login:fail:";
    private static final String LOCK_PREFIX = "exam:login:lock:";

    private final StringRedisTemplate redis;

    public LoginAttemptGuard(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean isLocked(String userNo) {
        return Boolean.TRUE.equals(redis.hasKey(LOCK_PREFIX + userNo));
    }

    /** 记录一次失败，达到阈值则锁定 */
    public void recordFailure(String userNo) {
        Long count = redis.opsForValue().increment(FAIL_PREFIX + userNo);
        if (Objects.equals(count, 1L)) {
            redis.expire(FAIL_PREFIX + userNo, Duration.of(10, ChronoUnit.MINUTES));
        }
    }

    /** 登录成功或管理员解锁时清零 */
    public void clear(String userNo) {
        redis.delete(FAIL_PREFIX + userNo);
    }
}
