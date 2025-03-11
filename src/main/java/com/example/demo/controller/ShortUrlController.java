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
@RequestMapping("/api/short-url")
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    public ShortUrlController(ShortUrlService shortUrlService) {
        this.shortUrlService=shortUrlService;
    }

    // 短链接跳转
    @Operation(summary = "短链接跳转", description = "访问短链接，自动跳转到原始长链接")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "跳转到长链接"),
            @ApiResponse(responseCode = "404", description = "短链接不存在"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @GetMapping("/{shortKey}")
    public ResponseEntity<ApiResponseDTO<String>> redirect(
            @PathVariable String shortKey) {

        if (shortKey == null || shortKey.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponseDTO<>(400, "短链接不存在", null));
        }

        String longUrl = shortUrlService.redirect(shortKey);
        if (longUrl.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(302).header("Location", longUrl).build();
    }

    @Operation(summary = "生成短链接", description = "输入长链接，生成唯一短链接")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功生成短链接"),
            @ApiResponse(responseCode = "400", description = "无效的 URL"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @PostMapping
    public ResponseEntity<ApiResponseDTO<String>> createShortUrl(
            @RequestBody Map<String, String> request) { // 如 longUrl = http://localhost/api/test/hello
        String longUrl =request.get("longUrl");

        if (longUrl == null || longUrl.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponseDTO<>(400, "无效的 URL", null));
        }

        String shortKey = shortUrlService.createShortUrl(longUrl);
        return ResponseEntity.ok(new ApiResponseDTO<>(200, "短链接生成成功", shortKey));
    }

    @Operation(summary = "获取访问次数", description = "查询短链接的访问统计数据")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功返回访问次数"),
            @ApiResponse(responseCode = "404", description = "短链接不存在"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @GetMapping("/stats/{shortKey}")
    public ResponseEntity<ApiResponseDTO<Map<String, Integer>>> getAccessCount(
            @PathVariable String shortKey) {

        if (shortKey == null || shortKey.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponseDTO<>(400, "短链接不存在", null));
        }

        Map<String, Integer> count = shortUrlService.getAccessCount(shortKey);
        return ResponseEntity.ok(new ApiResponseDTO<>(200, "访问次数获取成功", count));
    }

}
