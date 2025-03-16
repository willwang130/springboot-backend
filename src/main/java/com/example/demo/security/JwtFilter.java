package com.example.demo.security;

import com.example.demo.service.RedisService;
import io.jsonwebtoken.Claims;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisService redisService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {


        String token = extractToken(request);

        log.info("Received Token: {}", token);
        if (token != null) {
            try {
                // 1. 先查是否在黑名单
                if (redisService.isTokenBlacklisted(token)) {
                    log.warn("JWT 已被列入黑名单，拒绝访问");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token 已失效");
                    return;
                }

                // 2. 解析 token
                Claims claims = jwtUtil.parseToken(token);
                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                if (!role.startsWith("ROLE_")) {
                    role = "ROLE_" + role.toUpperCase();
                }

                // 3. 如果 token 有效, 设置 Spring Security 认证
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 从数据库获取完整的 UserDetails
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 把认证对象放入 SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT Token");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    public String extractToken(HttpServletRequest request) {
        // get Token from header
        String headerToken = Optional.ofNullable(request.getHeader("Authorization"))
                .filter(h -> h.startsWith("Bearer "))
                .map(h -> h.substring(7))
                .orElse(null);

        String cookieToken = Optional.ofNullable(request.getCookies())
                .flatMap(cookies -> Arrays.stream(cookies)
                        .filter(cookie -> "jwt".equals(cookie.getName()))
                        .map(Cookie::getValue)
                        .findFirst())
                .orElse(null);

        return headerToken != null ? headerToken : cookieToken;
    }
}
