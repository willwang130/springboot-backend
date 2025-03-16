package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.service.AuthService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;

import java.io.IOException;

@Tag(name = "2. 用户认证", description = "登录、注册、刷新 Token")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回 JWT Token 和 Refresh Token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<Void>> login(
            @RequestBody LoginRequest request, HttpServletResponse response) {
        return authService.login(request, response);
    }


    @Operation(summary = "用户登出", description = "清除 JWT Cookie")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登出成功")
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            HttpServletRequest request, HttpServletResponse response) {
        return authService.logout(request, response);
    }


    @Operation(summary = "用户注册", description = "创建新用户")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "注册成功"),
            @ApiResponse(responseCode = "400", description = "用户名已存在")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<Void>> register(
            @RequestBody @Validated RegisterRequest request) throws IOException {
        return authService.register(request);
    }

    @Operation(summary = "刷新 JWT Token", description = "使用 Refresh Token 获取新的 JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "JWT 刷新成功"),
            @ApiResponse(responseCode = "401", description = "Refresh Token 失效或缺失")
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            HttpServletRequest request, HttpServletResponse response) {
        return authService.refresh(request, response);
    }
}
