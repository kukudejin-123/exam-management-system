package com.exam.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置。绑定 application.yml 的 exam.jwt.* 段。
 */
@ConfigurationProperties(prefix = "exam.jwt")
public class JwtProperties {

    /** 签名密钥，生产环境必须通过环境变量注入，至少 32 字节 */
    private String secret = "change-me-in-production-0123456789abcdef";

    /** access token 有效期，分钟 */
    private long accessTtlMinutes = 15;

    /** refresh token 有效期，天 */
    private long refreshTtlDays = 7;

    /** token 签发方标识 */
    private String issuer = "exam-management-system";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTtlMinutes() {
        return accessTtlMinutes;
    }

    public void setAccessTtlMinutes(long accessTtlMinutes) {
        this.accessTtlMinutes = accessTtlMinutes;
    }

    public long getRefreshTtlDays() {
        return refreshTtlDays;
    }

    public void setRefreshTtlDays(long refreshTtlDays) {
        this.refreshTtlDays = refreshTtlDays;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
