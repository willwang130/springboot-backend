package com.example.demo.controller;


import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.repository.ShortUrlRepository;
import com.example.demo.service.TestService;
import com.example.demo.util.BloomFilterUtil;
import com.example.demo.util.RedisUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.swing.*;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@Tag(name = "3. 测试 API", description = "提供各种测试接口，如数据库、Redis 连接")
@Slf4j
@RequiredArgsConstructor
public class TestController {

    private final RedisUtil redisUtil;
    private final TestService testService;
    private final ShortUrlRepository shortUrlRepository;
    private final BloomFilterUtil bloomFilterUtil;


    @Operation(summary = "测试 Redis 连接", description = "返回 Redis 连接状态")
    @GetMapping("/redis")
    public ResponseEntity<String> testRedis() {
        return ResponseEntity.ok(redisUtil.testRedisConnection());
    }

    @Operation(summary = "测试数据库连接", description = "返回数据库连接状态")
    @GetMapping("/db")
    public ResponseEntity<String> testDatabase() {
        log.info("测试数据库连接...");
        return ResponseEntity.ok("数据库连接正常");
    }

    @Operation(summary = "测试 Admin 角色访问", description = "需要 ADMIN 角色才能访问")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin-check")
    public ResponseEntity<String> checkAdminAccess() {
        return ResponseEntity.ok("你是 ADMIN，访问通过");
    }

    @Operation(summary = "Say Hello", description = "Say Hello, World!")
    @GetMapping("/hello")
    public  ResponseEntity<String> sayHello() {
        return ResponseEntity.ok("Hello, World!");
    }

    @Operation(summary = "测试 log", description = "检查日志输出")
    @GetMapping("/log")
    public ResponseEntity<String> testLogging(){
        log.info("[INFO] 访问日志测试接口");
        log.debug("[DEBUG] 详细调试信息");
        log.warn("[WARN] 可能的问题");
        log.error("[ERROR] 发生错误");
        return ResponseEntity.ok("日志测试完成，检查日志输出");
    }

    @Operation(summary = "测试 性能", description = "量化 Redis, RabbitMQ 等的性能表现")
    @GetMapping("/performance/{i}")
    public ResponseEntity<String> performance(@PathVariable @NotNull int i) throws IOException {
        switch (i) {
            case 1:
                return testService.measureQueryPerformance();
            case 2:
                return testService.measureRabbitMQImpact();
            case 3:
                return testService.measureBatchProcessImpact();
            default:
                return ResponseEntity.badRequest().body("无效的测试编号");
        }
    }
    @Operation(summary = "重置所有访问计数", description = "清空 MySQL `access_count` 和 Redis `total_hits_count`")
    @Transactional
    @DeleteMapping("/reset-all")
    public ResponseEntity<String> resetAll() {
        // 清空 MySQL 访问计数
        shortUrlRepository.resetAllAccessCounts();

        // 清空 Redis 访问计数
        redisUtil.deleteCache("total_hits_count:");

        return ResponseEntity.ok("成功清空 MySQL `access_count` 和 Redis `total_hits_count`");
    }

    @Operation(summary = "重建 Bloom Filter", description = "删除已有 手动重建 返回新存 id 数量")
    @PostMapping("/rest-bloom")
    public  ResponseEntity<ApiResponseDTO<Map<String, Integer>>> resetBloomFilterManually() {
        return bloomFilterUtil.resetBloomFilter();
    }

}

