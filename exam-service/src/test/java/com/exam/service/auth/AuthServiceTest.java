package com.exam.service.auth;

import com.exam.common.exception.BizException;
import com.exam.repository.entity.User;
import com.exam.repository.mapper.UserMapper;
import com.exam.security.jwt.JwtService;
import com.exam.security.jwt.JwtProperties;
import com.exam.security.jwt.TokenBlacklist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 注册与登录单元测试。
 *
 * <p>Mapper 与 Redis 全部 Mock 掉——登录逻辑的分支（锁定、密码错、账号停用、
 * 登出拉黑）不该依赖外部中间件，否则测试又慢又难定位。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET = "auth-test-secret-key-0123456789abcdef";

    @Mock
    private UserMapper userMapper;
    @Mock
    private TokenBlacklist blacklist;
    @Mock
    private LoginAttemptGuard attemptGuard;

    private PasswordEncoder encoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        jwtService = new JwtService(props);
        authService = new AuthService(userMapper, encoder, jwtService, blacklist, attemptGuard);
    }

    private User existingUser(String userNo, String rawPassword, int status) {
        User u = new User();
        u.setId(99L);
        u.setUserNo(userNo);
        u.setName("测试用户");
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setRole("STUDENT");
        u.setClassId(7L);
        u.setStatus(status);
        return u;
    }

    @Test
    @DisplayName("正确凭据登录成功，返回双 token")
    void login_success() {
        when(userMapper.selectOne(any())).thenReturn(existingUser("S001", "123456", 1));

        LoginResult result = authService.login("S001", "123456");

        assertThat(result.userId()).isEqualTo(99L);
        assertThat(result.role()).isEqualTo("STUDENT");
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.accessExpiresIn()).isEqualTo(900);
        // 登录成功后要清失败计数
        verify(attemptGuard).clear("S001");
    }

    @Test
    @DisplayName("密码错误抛 BAD_CREDENTIAL 并记录一次失败")
    void wrong_password_records_failure() {
        when(userMapper.selectOne(any())).thenReturn(existingUser("S001", "123456", 1));

        assertThatThrownBy(() -> authService.login("S001", "wrong"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("学号或密码错误");
        verify(attemptGuard).recordFailure("S001");
    }

    @Test
    @DisplayName("用户不存在时同样记录失败，避免枚举用户名")
    void unknown_user_records_failure() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> authService.login("NOPE", "123456"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("学号或密码错误");
        verify(attemptGuard).recordFailure("NOPE");
    }

    @Test
    @DisplayName("账号锁定时直接拒绝，不再查库")
    void locked_account_rejected() {
        when(attemptGuard.isLocked("S001")).thenReturn(true);

        assertThatThrownBy(() -> authService.login("S001", "123456"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("锁定");
        verify(userMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("停用账号不能登录")
    void disabled_account_rejected() {
        when(userMapper.selectOne(any())).thenReturn(existingUser("S001", "123456", 0));

        assertThatThrownBy(() -> authService.login("S001", "123456"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已停用");
    }

    @Test
    @DisplayName("注册时学号已存在则拒绝")
    void duplicate_user_no_rejected() {
        when(userMapper.exists(any())).thenReturn(true);

        assertThatThrownBy(() ->
                authService.register("S001", "张三", "123456", "STUDENT", 7L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已被注册");
    }

    @Test
    @DisplayName("非学生角色的 classId 被强制置空，密码不明文存储")
    void teacher_class_id_cleared() {
        when(userMapper.exists(any())).thenReturn(false);
        // 模拟 MyBatis 回填自增主键，否则注册返回值拿不到 id
        org.mockito.Mockito.doAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(123L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        long id = authService.register("T001", "张老师", "123456", "TEACHER", 7L);

        assertThat(id).isEqualTo(123L);
        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getClassId()).isNull();
        assertThat(saved.getPasswordHash()).isNotEqualTo("123456");
        assertThat(encoder.matches("123456", saved.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("登出把 token 加入黑名单")
    void logout_blacklists_token() {
        String token = jwtService.issueAccessToken(99L, "STUDENT", 7L, jwtService.newJti());

        authService.logout(token);

        verify(blacklist).revoke(anyString(), org.mockito.ArgumentMatchers.anyLong());
    }
}
