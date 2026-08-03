package cn.luyou.utils;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 工具类
 * 密钥从配置文件读取，避免硬编码
 *
 * @author ruolifeng
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire:604800000}")
    private long expire;

    /**
     * 生成 Token
     *
     * @param userId 用户 ID
     * @return JWT Token 字符串
     */
    public String generateToken(Long userId) {
        // 以字符串写入，避免雪花 ID 在 JSON number 中精度丢失
        return JWT.create()
                .setPayload("userId", String.valueOf(userId))
                .setExpiresAt(new java.util.Date(System.currentTimeMillis() + expire))
                .setKey(secret.getBytes())
                .sign();
    }

    /**
     * 解析 Token，返回 JWT 对象
     *
     * @param token Token 字符串
     * @return JWT 对象
     */
    public JWT parseToken(String token) {
        return JWTUtil.parseToken(token);
    }

    /**
     * 验证 Token 是否合法（签名 + 有效期）
     *
     * @param token Token 字符串
     * @return 是否合法
     */
    public boolean verifyToken(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            return jwt.setKey(secret.getBytes()).verify() && jwt.validate(0);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 Token 中获取用户 ID
     *
     * @param token Token 字符串
     * @return 用户 ID
     */
    public Long getUserIdFromToken(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        Object userId = jwt.getPayload("userId");
        return Long.parseLong(userId.toString());
    }
}
