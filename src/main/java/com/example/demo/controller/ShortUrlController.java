package com.example.demo.controller;

import com.example.demo.service.ShortUrlService;
import com.example.demo.dto.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "短链接管理", description = "提供短链接生成, 跳转, 访问统计等 API")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/short-url")
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    // 短链接跳转
    @Operation(summary = "短链接跳转", description = "访问短链接，自动跳转到原始长链接")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "跳转到长链接"),
            @ApiResponse(responseCode = "404", description = "短链接不存在"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @GetMapping("/{shortKey}")
    public ResponseEntity<ApiResponseDTO<String>> redirect(
            @PathVariable @NotBlank String shortKey) {
        return shortUrlService.redirect(shortKey);
    }


    @Operation(summary = "生成短链接", description = "输入长链接，生成唯一短链接")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功生成短链接"),
            @ApiResponse(responseCode = "400", description = "无效的 URL"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @PostMapping
    public ResponseEntity<ApiResponseDTO<String>> createShortUrl(
            @RequestBody Map<String, @NotBlank String> request) { // 如 longUrl = http://localhost/api/test/hello
        return shortUrlService.createShortUrl(request.get("longUrl"));
    }


    @Operation(summary = "获取访问次数", description = "查询短链接的访问统计数据")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功返回访问次数"),
            @ApiResponse(responseCode = "404", description = "短链接不存在"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @GetMapping("/stats/{shortKey}")
    public ResponseEntity<ApiResponseDTO<Map<String, Integer>>> getAccessCount(
            @PathVariable @NotBlank String shortKey) {
        return shortUrlService.getAccessCount(shortKey);
    }

}
