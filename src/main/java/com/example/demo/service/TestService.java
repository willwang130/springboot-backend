package com.example.demo.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.example.demo.dto.ProductDTO;
import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ShortUrlRepository;
import com.example.demo.webSocket.WebSocketNotificationHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestService {

    private final ProductRepository productRepository;
    private final ShortUrlRepository shortUrlRepository;
    private final RedisService redisService;
    private final WebSocketNotificationHandler notificationHandler;

    // 1. 测试 Redis vs MySQL 查询
    public ResponseEntity<String> measureQueryPerformance() {
        String cacheKey = "product:";
        Long id = productRepository.findFirstValidProductId()
                .filter( idl -> redisService.isKeyExists(cacheKey + idl))
                .orElse(null);
        if (id == null) {
            return ResponseEntity.notFound().build();
        }

        // 记录 Redis 查询时间
        Instant startRedis = Instant.now();
        ProductDTO redisProduct = redisService.getFromCacheWithType(cacheKey + id, new TypeReference<ProductDTO>() {});
        Instant endRedis = Instant.now();
        long redisTime = Duration.between(startRedis, endRedis).toMillis();

        // 记录 MySQL 查询时间
        Instant startDB = Instant.now();
        Optional<Product> dbProduct = productRepository.findById(id);
        Instant endDB = Instant.now();
        long dbTime = Duration.between(startDB,endDB).toMillis();

        // 计算查询优化比例
        double improvement = dbTime > 0 ? ((double) (dbTime - redisTime) / dbTime) * 100 : 0;

        return ResponseEntity.ok(String.format("Redis vs MySQL 查询: Redis %dms, MySQL %dms, 提升 %.2f%%",
                redisTime, dbTime, improvement));
    }

    // 2. 测试 RabbitMQ + Redis List 对数据库写入的减少
    public ResponseEntity<String> measureRabbitMQImpact() {
        long redisListSize = redisService.getBufferListSize("short_url_buffer");
        long redisProcessed = redisService.getTotalHitsCount();
        long dbWritesLast10Min = shortUrlRepository.sumUpdatesLastNMin(10).orElse(0L);

        double reduction10Min = ((double) (redisProcessed - dbWritesLast10Min) / Math.max(redisProcessed, 1)) * 100;

        return ResponseEntity.ok(String.format(
                "Redis List 作为缓冲区 | 当前Redis List队列长度: %d | Redis 计数: %d " +
                        "| MySQL 更新 (10 分钟): %d | 降低 (10 分钟) %.2f%%",
                redisListSize, redisProcessed,
                 dbWritesLast10Min, reduction10Min));
    }

    // 3. 测试批量写入优化
    public ResponseEntity<String> measureBatchProcessImpact() {
        long redisCounts = redisService.getTotalHitsCount();
        long dbWritesLast10Min  = shortUrlRepository.sumUpdatesLastNMin(10).orElse(0L);

        if (redisCounts == 0 || dbWritesLast10Min  == 0) {
            return ResponseEntity.ok("Redis 或 MySQL 没有数据，测试无效");
        }

        double throughputBoost = (double) redisCounts / dbWritesLast10Min ;

        return ResponseEntity.ok(String.format("全局批量写入优化: Redis 计数 %d, MySQL 总写入 %d, 吞吐量提升 %.2fx",
                redisCounts, dbWritesLast10Min, throughputBoost));
    }

    public String getLiveStats() {
        long redisListSize = redisService.getBufferListSize("short_url_buffer");
        long redisProcessed = redisService.getTotalHitsCount();
        long dbWritesLast10Min = shortUrlRepository.sumUpdatesLastNMin(10).orElse(0L);

        double reduction10Min = ((double) (redisProcessed - dbWritesLast10Min) / Math.max(redisProcessed, 1)) * 100;

        return String.format(
                "Redis List 长度: %d | Redis 计数: %d | MySQL 更新 (10 分钟): %d | 降低 (10 分钟): %.2f%%",
                redisListSize, redisProcessed, dbWritesLast10Min, reduction10Min
        );
    }

    // **每秒推送 Test 2 & Test 3 结果**
    @Scheduled(fixedRate = 1000) // 每秒执行
    public void pushTestResultsToWebSocket() throws IOException {

        // 1. 获取 Redis 和 MySQL 数据
        long redisListSize = redisService.getBufferListSize("short_url_buffer:");
        long redisProcessed = redisService.getTotalHitsCount();
        long dbWritesLast10Min = shortUrlRepository.sumUpdatesLastNMin(10).orElse(0L);

        // 2.3. 计算数据, 生成 JSON 格式的 websocket 消息
        String message = generateTestResultsMessage(redisListSize, redisProcessed, dbWritesLast10Min);


        // 4. 确保 WebSocket 发送不会因异常失败
        try {
            notificationHandler.sendNotification(message);
        } catch (Exception e) {
            log.error("WebSocket 发送失败: {}", e.getMessage());
        }

    }

    private static String generateTestResultsMessage(long redisListSize, long redisProcessed, long dbWritesLast10Min) {


        double reduction10Min = redisProcessed > 0
                ?((double) (redisProcessed - dbWritesLast10Min) / redisProcessed) * 100
                : 0;

        double throughputBoost = dbWritesLast10Min > 0
                ? (double) redisProcessed / dbWritesLast10Min
                : 0;

        // JSON
        return String.format(
                "{ \"type\": \"test\", " +
                "  \"test2\": \"Redis List 缓冲区: 当前长度 %d, Redis 计数 %d, MySQL 更新 (10 分钟) %d, 降低 %.2f%%\", " +
                "  \"test3\": \"批量写入优化: Redis 计数 %d, MySQL 总写入 %d, 吞吐量提升 %.2fx\" }",
                redisListSize, redisProcessed, dbWritesLast10Min, reduction10Min,
                redisProcessed, dbWritesLast10Min, throughputBoost
        );
    }

}
