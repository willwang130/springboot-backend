package com.example.demo.security;

import com.example.demo.service.RedisService;
import com.example.demo.util.RedisUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final RedisService redisService;
    // 从 application.properties 读取配置
    @Value("${jwt.secret}")
    private String secretKey;
    @Value("${jwt.expiration}")
    private long expiration;
    @Value("${jwt.refreshExpiration}")
    private long refreshExpiration;

    public String generateToken(String username, String role) {
        return createToken(username, role, expiration);
    }

    public String generateRefreshToken(String username) {
        return createToken(username, null, refreshExpiration);
    }

    // 生成 JWT 令牌
    public String createToken(String username, String role, long expiration) {

        SecretKey key = Keys.hmacShaKeyFor((secretKey.getBytes(StandardCharsets.UTF_8)));

        JwtBuilder builder = Jwts.builder()
                .setSubject(username)               // 设定主题 （用户名）
                .claim("role", role)             // “role” = role
                .setIssuedAt(new Date())            // Token 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // 过期时间
                .signWith(key, SignatureAlgorithm.HS256 ); // 用密钥签名

        if (role != null) {
            builder.claim("role", role);
        }

        // 生成最终 Token
        return builder.compact();
    }

    // 解析 JWT 令牌
    public Claims parseToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor((secretKey.getBytes(StandardCharsets.UTF_8)));
            return Jwts.parserBuilder()         // 创建 JwtParserBuilder
                    .setSigningKey(key)         // 设置密钥（用于验证签名）
                    .build()                    // 创建 JwtParser 实例
                    .parseClaimsJws(token)      // 解析 Token，检查签名和有效期
                    .getBody();                 // 获取 Payload
        }catch (ExpiredJwtException e) {
            System.out.println("JWT Expired: " + e.getMessage()); //  Token 过期
            throw e;
        } catch (UnsupportedJwtException e) {
            System.out.println("Unsupported JWT: " + e.getMessage()); //  不支持的 JWT
            throw e;
        } catch (MalformedJwtException e) {
            System.out.println("Malformed JWT: " + e.getMessage()); //  Token 格式错误
            throw e;
        } catch (SignatureException e) {
            System.out.println("Invalid JWT Signature: " + e.getMessage()); //  签名错误
            throw e;
        } catch (Exception e) {
            System.out.println("JWT Parsing Failed: " + e.getMessage()); //  其他错误
            throw e;
        }
    }

    public long getExpirationFromToken(String token) {
        Claims claim = parseToken(token);
        return claim.getExpiration().getTime() - System.currentTimeMillis();
    }

    public long getExpiration(String token) {
        return getExpirationFromToken(token);
    }
}
