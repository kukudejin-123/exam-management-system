package com.exam.service.auth;

import com.exam.common.constant.ErrorCode;
import com.exam.common.constant.Role;
import com.exam.common.exception.BizException;
import com.exam.repository.entity.User;
import com.exam.repository.mapper.UserMapper;
import com.exam.security.jwt.JwtService;
import com.exam.security.jwt.TokenBlacklist;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 注册与登录。
 *
 * <p>对应实训内容 4 的"用户的注册及登录"。
 * 登录失败次数限制（5 次锁 10 分钟）是账号安全的底线，实现放在
 * {@link LoginAttemptGuard}，便于单测替换 Redis。
 */
@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 10;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklist blacklist;
    private final LoginAttemptGuard attemptGuard;

    public AuthService(UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       TokenBlacklist blacklist,
                       LoginAttemptGuard attemptGuard) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.blacklist = blacklist;
        this.attemptGuard = attemptGuard;
    }

    /**
     * 注册。学号全局唯一，重复注册直接拒绝而不是覆盖。
     */
    @Transactional
    public long register(String userNo, String name, String rawPassword, String roleName, Long classId) {
        if (userMapper.exists(Wrappers.<User>lambdaQuery().eq(User::getUserNo, userNo))) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "学号/工号 " + userNo + " 已被注册");
        }
        Role role = Role.from(roleName);

        User user = new User();
        user.setUserNo(userNo);
        user.setName(name);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role.name());
        user.setClassId(Role.STUDENT == role ? classId : null);
        user.setStatus(1);
        userMapper.insert(user);
        return user.getId();
    }

    /**
     * 登录。返回 access + refresh 双 token。
     *
     * @throws BizException 账号锁定 / 密码错误 / 账号停用
     */
    public LoginResult login(String userNo, String rawPassword) {
        if (attemptGuard.isLocked(userNo)) {
            throw new BizException(ErrorCode.LOGIN_LOCKED,
                    "登录失败次数过多，账号已锁定 " + LOCK_MINUTES + " 分钟");
        }

        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUserNo, userNo));
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            attemptGuard.recordFailure(userNo);
            throw new BizException(ErrorCode.BAD_CREDENTIAL, "学号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已停用");
        }

        attemptGuard.clear(userNo);

        String accessJti = jwtService.newJti();
        String access = jwtService.issueAccessToken(
                user.getId(), user.getRole(), user.getClassId(), accessJti);
        String refresh = jwtService.issueRefreshToken(user.getId(), jwtService.newJti());

        return LoginResult.of(access, jwtService.accessTtlSeconds(), refresh,
                user.getId(), user.getRole());
    }

    /**
     * 用 refresh token 换新的 access token。
     * refresh token 本身也进黑名单，一次一换，降低长期有效的风险。
     */
    public LoginResult refresh(String refreshToken) {
        var claims = jwtService.parse(refreshToken);
        if (blacklist.isRevoked(claims.getId())) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED, "登录态已失效，请重新登录");
        }
        long userId = Long.parseLong(claims.getSubject());
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        blacklist.revoke(claims.getId(), jwtService.accessTtlSeconds() * 60);

        String accessJti = jwtService.newJti();
        String access = jwtService.issueAccessToken(
                user.getId(), user.getRole(), user.getClassId(), accessJti);
        String newRefresh = jwtService.issueRefreshToken(user.getId(), jwtService.newJti());

        return LoginResult.of(access, jwtService.accessTtlSeconds(), newRefresh,
                user.getId(), user.getRole());
    }

    /** 登出：把 token 加入黑名单，剩余有效期内立即失效 */
    public void logout(String accessToken) {
        var claims = jwtService.parse(accessToken);
        long ttl = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
        blacklist.revoke(claims.getId(), ttl);
    }

    /** 最近登录时间更新，供"最后登录"展示。失败不影响登录结果。 */
    public void touchLastLogin(long userId) {
        User update = new User();
        update.setId(userId);
        update.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(update);
    }
}
