package com.example.demo.service;


import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.dto.ProductDTO;
import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ShortUrlRepository;
import com.example.demo.util.RedisUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TestService {

    private final ProductRepository productRepository;
    private final ShortUrlRepository shortUrlRepository;
    private final RedisUtil redisUtil;

    // 1. 测试 Redis vs MySQL 查询
    public ResponseEntity<String> measureQueryPerformance() {
        String cacheKey = "product:";
        Long id = productRepository.findFirstValidProductId()
                .filter( idl -> redisUtil.exists(cacheKey + idl))
                .orElse(null);
        if (id == null) {
            return ResponseEntity.notFound().build();
        }

        // 记录 Redis 查询时间
        Instant startRedis = Instant.now();
        ProductDTO redisProduct = redisUtil.getObjectByKeyAndConvert(cacheKey + id, new TypeReference<ProductDTO>() {});
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
        long redisListSize = redisUtil.getListSize("short_url_buffer");
        long redisProcessed = redisUtil.getTotalHitsCount();
        long dbWritesLast10Min = shortUrlRepository.sumUpdatesLastNMin(10);

        double reduction10Min = ((double) (redisProcessed - dbWritesLast10Min) / Math.max(redisProcessed, 1)) * 100;

        return ResponseEntity.ok(String.format(
                "Redis List 作为缓冲区 | 当前Redis List队列长度: %d | Redis 计数: %d " +
                        "| MySQL 更新 (10 分钟): %d | 降低 (10 分钟) %.2f%%",
                redisListSize, redisProcessed,
                 dbWritesLast10Min, reduction10Min));
    }

    // 3. 测试批量写入优化
    public ResponseEntity<String> measureBatchProcessImpact() {
        long redisCounts = redisUtil.getTotalHitsCount();
        long dbWritesLast10Min  = shortUrlRepository.sumUpdatesLastNMin(10);

        if (redisCounts == 0 || dbWritesLast10Min  == 0) {
            return ResponseEntity.ok("Redis 或 MySQL 没有数据，测试无效");
        }

        double throughputBoost = (double) redisCounts / dbWritesLast10Min ;

        return ResponseEntity.ok(String.format("全局批量写入优化: Redis 计数 %d, MySQL 总写入 %d, 吞吐量提升 %.2fx",
                redisCounts, dbWritesLast10Min, throughputBoost));
    }
}
