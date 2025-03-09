package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.User;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtil;
import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.webSocket.WebSocketNotificationHandler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import io.swagger.v3.oas.annotations.Operation;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Tag(name = "2. 用户认证", description = "登录、注册、刷新 Token")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final WebSocketNotificationHandler notificationHandler;


    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回 JWT Token 和 Refresh Token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<Void>> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            return ResponseEntity
                    .status(401)
                    .body(new ApiResponseDTO<>(401, "用户名或密码错误", null));
        }

        User user = userOpt.get();
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        String refresh_token = jwtUtil.generateRefreshToken(user.getUsername());

        // Set Cookie
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", token)
                .httpOnly(true) // 防止 JavaScript访问Cookie, 避免 XSS
                .secure(false) // HTTPS ONLY
                .path("/")
                .maxAge(60 * 15)
                .build();
        // Set Refresh Token
        ResponseCookie refreshCookie = ResponseCookie.from("refresh-token", refresh_token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(60 * 60 * 24)
                .build();
        // 写入 Cookie
        response.addHeader("Set-Cookie", jwtCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

        log.info("用户 {} 登录成功", user.getUsername());

        System.out.println("Password match failed! User entered: " + request.getPassword());
        System.out.println("Stored hash: " + user.getPassword());

        return ResponseEntity.ok(new ApiResponseDTO<>(200, "登录成功", null));
    }


    @Operation(summary = "用户登出", description = "清除 JWT Cookie")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)         // 防止 JavaScript 访问
                .secure(false)
                .path("/")
                .maxAge(0) // Cookie 失效
                .build();
        response.addHeader("Set-Cookie", jwtCookie.toString()); // 执行删除 JWT

        return ResponseEntity.ok(new ApiResponseDTO<>(200, "注销成功", null));
    }


    @Operation(summary = "用户注册", description = "创建新用户")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "注册成功"),
            @ApiResponse(responseCode = "400", description = "用户名已存在")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<Void>> register(@RequestBody @Validated RegisterRequest request) throws IOException {

        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(new ApiResponseDTO<>(400, "用户名已存在", null));
        }
        User newUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();
        userRepository.save(newUser);

        // **WebSocket 发送通知**
        notificationHandler.sendNotification("新用户注册: " + request.getUsername());

        return ResponseEntity.ok(new ApiResponseDTO<>(200, "注册成功", null));
    }

    @Operation(summary = "刷新 JWT Token", description = "使用 Refresh Token 获取新的 JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "JWT 刷新成功"),
            @ApiResponse(responseCode = "401", description = "Refresh Token 失效或缺失")
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {

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

            // 更新 JWT Cookie
            ResponseCookie jwtCookie = ResponseCookie.from("jwt", newJwtToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(60 * 15)
                    .build();
            // 更新 Refresh Token Cookie
            ResponseCookie refreshCookie = ResponseCookie.from("refresh-token", newRefreshToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(60 * 60 * 24)
                    .build();

            response.addHeader("Set-Cookie", jwtCookie.toString());
            response.addHeader("Set-Cookie", refreshCookie.toString());

            log.info("用户 {} 成功刷新 JWT Token", username);

            return ResponseEntity.ok(new ApiResponseDTO<>(200, "JWT 刷新成功", null));

        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ApiResponseDTO<>(401, "Refresh Token 已过期", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ApiResponseDTO<>(401, "无效的 Refresh Token", null));
        }
    }
}
