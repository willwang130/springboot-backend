package com.example.demo.service;

import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.entity.ShortUrl;
import com.example.demo.repository.ShortUrlRepository;
import com.example.demo.service.RabbitMQ.RabbitMQProducer;
import com.example.demo.util.BloomFilterUtil;
import com.example.demo.util.RedisUtil;
import com.example.demo.util.ShortKeyGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortUrlService {

    private final ShortUrlRepository shortUrlRepository;
    private final RedisService redisService;
    private final RabbitMQProducer rabbitMQProducer;
    private final BloomFilterUtil bloomFilterUtil;
    private static final String REDIS_KEY_PREFIX = "short_url:";
    private static final String REDIS_HITS_PREFIX = "short_url_hits:";
    private static final String BLOOM_FILTER_NAME_SHORT = "bloom:shortUrl:";


    public ResponseEntity<ApiResponseDTO<String>> redirect(String shortKey) {

        // 1. 先查询 Bloom 过滤器
        if (!bloomFilterUtil.mightContain(BLOOM_FILTER_NAME_SHORT, shortKey)) {
            return ResponseEntity.status(404).body(new ApiResponseDTO<>(404, "短链接不存在", null));
        }

        // 2. 查 Redis 缓存
        String longUrl = redisService.getFromCacheWithType(REDIS_KEY_PREFIX + shortKey, new TypeReference<String>() {});

        // 3. 如果 Redis 没有
        if (longUrl == null) {
            // 再查数据库
            Optional<ShortUrl> optional = shortUrlRepository.findByShortKey(shortKey);
            if (optional.isEmpty()) {
                return ResponseEntity.status(404).body(new ApiResponseDTO<>(404, "短链接不存在", null));
            }
            // 存入 Redis
            longUrl = optional.get().getLongUrl();
            redisService.setFromCacheWithObject(REDIS_KEY_PREFIX + shortKey, longUrl, 7, TimeUnit.DAYS);
        }

        // 4. 统计访问次数 发送消息到 RabbitMQ
        log.info("Producer 发送消息: {}", shortKey);
        rabbitMQProducer.sendMessage(shortKey);

        // redisUtil.increment(REDIS_HITS_PREFIX + shortKey); //先存 Redis, 之后每10分钟 存1次数据库

        // 302 导航至 longUrl
        return ResponseEntity.status(302).header("Location", longUrl).build();
    }

    @Transactional
    public ResponseEntity<ApiResponseDTO<String>> createShortUrl(String longUrl) {
        // 1. 先查 Redis 缓存
        String shortKey = redisService.getFromCacheWithType(REDIS_KEY_PREFIX + "longUrl:" +longUrl, new TypeReference<String>() {});
        if(shortKey != null) {
            return ResponseEntity.ok(new ApiResponseDTO<>(200, "Redis 短链接已存在", shortKey));
        }

        // 2. 查询数据库是否已有相同 longUrl      Redis 会过期
        ShortUrl existing = shortUrlRepository.findByLongUrl(longUrl);
        if (existing != null) {
            // 存 Redis 1天期限 防止重复创建
            shortKey = existing.getShortKey();
            redisService.setFromCacheWithObject(REDIS_KEY_PREFIX + "longUrl:" + longUrl, shortKey, 1, TimeUnit.DAYS);
            return ResponseEntity.ok(new ApiResponseDTO<>(200, "MySQL 短链接已存在", shortKey)); // 直接返回已有短链接
        }

        // 3. 生成短链接 Key (BASE62 + 雪花算法)
        shortKey = ShortKeyGenerator.generate();
        ShortUrl entity = ShortUrl.builder()
                .shortKey(shortKey)
                .longUrl(longUrl)
                .build();
        // 4. 存入数据库
        shortUrlRepository.save(entity);

        // 5. 添加到 Bloom Filter
        bloomFilterUtil.addToBloomFilter(BLOOM_FILTER_NAME_SHORT, shortKey);


        // 6. 存入 Redis 缓存
        redisService.setFromCacheWithObject(REDIS_KEY_PREFIX + shortKey, longUrl, 7, TimeUnit.DAYS);

        return ResponseEntity.ok(new ApiResponseDTO<>(200, "短链接生成成功", shortKey));
    }


    public ResponseEntity<ApiResponseDTO<Map<String, Integer>>> getAccessCount(String shortKey) {
        Map<String, Integer> result = new HashMap<>();

        // 查 Redis
        String redisCount = redisService.getStringWithKey(REDIS_HITS_PREFIX + shortKey);
        int redisValue = redisCount != null ? Integer.parseInt(redisCount) : 0;
        result.put("redis",redisValue);

        // 查 数据库
        int mysqlValue = shortUrlRepository.findByShortKey(shortKey)
                .map(ShortUrl::getAccessCount)
                .orElse(0);
        result.put("mysql", mysqlValue);

        return  ResponseEntity.ok(new ApiResponseDTO<>(200, "访问次数获取成功", result));
    }
}
