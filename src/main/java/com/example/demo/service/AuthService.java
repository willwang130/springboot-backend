package com.example.demo.service;

import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.User;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtil;
import com.example.demo.webSocket.WebSocketNotificationHandler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisService redisService;
    private final WebSocketNotificationHandler notificationHandler;

    public ResponseEntity<ApiResponseDTO<Void>> login(
            @RequestBody LoginRequest request, HttpServletResponse response) {

        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isEmpty() || !passwordEncoder.matches(
                request.getPassword(), userOpt.get().getPassword())) {
            return ResponseEntity
                    .status(401)
                    .body(new ApiResponseDTO<>(401, "用户名或密码错误", null));
        }

        User user = userOpt.get();
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        String refresh_token = jwtUtil.generateRefreshToken(user.getUsername());


        // 生成 Cookie 并设置到 Response
        setJwtCookies(response, token, refresh_token);


        log.info("用户 {} 登录成功", user.getUsername());
        return ResponseEntity.ok(new ApiResponseDTO<>(200, "登录成功", null));
    }

    public ResponseEntity<?> logout(
            HttpServletRequest request, HttpServletResponse response) {

        // request 里取出 String Token
        String jwt = getJwtFromRequest(request);
        String refreshToken = getRefreshFromRequest(request);

        // JWT + refresh 添加进 redis 黑名单
        if (jwt != null) {
            redisService.invalidTokenToBlackList(jwt, jwtUtil.getExpiration(jwt));;
        }
        if (refreshToken != null) {
            redisService.invalidTokenToBlackList(refreshToken, jwtUtil.getExpiration(jwt));
        }

        // 执行无效化 JWT 和 refresh
        response.addHeader("Set-Cookie", createExpiredCookie(jwt).toString());
        response.addHeader("Set-Cookie", createExpiredCookie(refreshToken).toString());

        return ResponseEntity.ok(new ApiResponseDTO<>(200, "登出成功", null));
    }


    public ResponseEntity<ApiResponseDTO<Void>> register(
            @RequestBody @Validated RegisterRequest request) throws IOException {

        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO<>(400, "用户名已存在", null));
        }
        User newUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();
        userRepository.save(newUser);

        // WebSocket 发送通知
        notificationHandler.sendNotification("新用户注册: " + request.getUsername());

        return ResponseEntity.ok(new ApiResponseDTO<>(200, "注册成功", null));
    }

    public ResponseEntity<?> refresh(
            HttpServletRequest request, HttpServletResponse response) {
        // 从 Cookie 读取 Refresh Token
        String refreshToken = Optional.ofNullable(request.getCookies())
                .flatMap(cookies -> Arrays.stream(request.getCookies())
                        .filter(c -> "refresh-token".equals(c.getName()))
                        .findFirst()
                        .map(Cookie::getValue))
                .orElse(null);

        if (refreshToken == null) {
            log.warn("Refresh Token 为空，无法刷新 JWT");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDTO<>(401, "Refresh Token 缺失", null));
        }

        // 解析 Refresh Token
        try {
            Claims claims = jwtUtil.parseToken(refreshToken);
            String username = claims.getSubject();

            // 检查 Refresh Token 是否有效
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UserNotFoundException("用户 " + username + " 未找到"));

            String newJwtToken = jwtUtil.generateToken(username, user.getRole());
            String newRefreshToken = jwtUtil.generateRefreshToken(username);

            // 生成 Cookie 并设置到 Response
            setJwtCookies(response,newJwtToken, newRefreshToken);

            return ResponseEntity.ok(new ApiResponseDTO<>(200, "JWT 刷新成功", null));

        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ApiResponseDTO<>(401, "Refresh Token 已过期", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ApiResponseDTO<>(401, "无效的 Refresh Token", null));
        }
    }


    private void setJwtCookies(HttpServletResponse response, String token, String refresh_token) {
        response.addHeader("Set-Cookie", createJwtCookie(token).toString());
        response.addHeader("Set-Cookie", createRefreshCookie(refresh_token).toString());
    }

    private ResponseCookie createJwtCookie(String token) {
        return ResponseCookie
                .from("jwt", token)
                .httpOnly(true).secure(false)
                .path("/")
                .maxAge(60 * 15)
                .build();
    }

    private ResponseCookie createRefreshCookie(String refresh_token) {
        return ResponseCookie.from("refresh-token", refresh_token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(60 * 60 * 24)
                .build();
    }

    public String getJwtFromRequest(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies())                // 获取 Cookie 数组
                .flatMap(cookie -> Arrays.stream(cookie)        // 遍历 Cookie
                        .filter(c -> "jwt".equals(c.getName()))  // 找到 jwt token
                        .findFirst()
                        .map(Cookie::getValue))                         //  获取值
                .orElse(null);
    }

    public String getRefreshFromRequest(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies())
                .flatMap(cookie -> Arrays.stream(cookie)
                        .filter(c -> "refresh-token".equals(c.getName()))
                        .findFirst()
                        .map(Cookie::getValue))
                .orElse(null);
    }

    public ResponseCookie createExpiredCookie(String tokenName) {
        return ResponseCookie.from(tokenName, "")   //  该 token 设为空值
                .httpOnly(true)                           //  防止 JavaScript 访问
                .secure(false)                            //  是否仅限 HTTPS 传输 (生产环境改为 true)
                .path("/")                                //  所有地址可用
                .maxAge(0)                  //  cookie 到期失效
                .build();
    }

}
