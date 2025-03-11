package com.example.demo.service;

import com.example.demo.entity.ShortUrl;
import com.example.demo.repository.ShortUrlRepository;
import com.example.demo.service.RabbitMQ.RabbitMQProducer;
import com.example.demo.util.RedisUtil;
import com.example.demo.util.ShortKeyGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final RedisUtil redisUtil;
    private final RabbitMQProducer rabbitMQProducer;
    private static final String REDIS_KEY_PREFIX = "short_url:";
    private static final String REDIS_HITS_PREFIX = "short_url_hits:";



    public String redirect(String shortKey) {
        // 1. 先查 Redis 缓存
        String longUrl = redisUtil.getObjectByKeyAndConvert(REDIS_KEY_PREFIX + shortKey, new TypeReference<String>() {});

        // 2. 如果 Redis 没有 再查数据库
        if (longUrl == null) {
            Optional<ShortUrl> optional = shortUrlRepository.findByShortKey(shortKey);
            if (optional.isEmpty()) {
                return "";
            }
            longUrl = optional.get().getLongUrl();
            // 存入 Redis
            redisUtil.setObjectByKey(REDIS_KEY_PREFIX + shortKey, longUrl, 7, TimeUnit.DAYS);
        }

        // 3. 统计访问次数 发送访问统计到 RabbitMQ
        log.info("Producer 发送消息: {}", shortKey);
        rabbitMQProducer.sendMessage(shortKey);

        // redisUtil.increment(REDIS_HITS_PREFIX + shortKey); //先存 Redis, 之后每10分钟 存1次数据库

        return longUrl;
    }

    @Transactional
    public String createShortUrl(String longUrl) {
        // 先查 Redis 缓存
        String shortKey = redisUtil.getObjectByKeyAndConvert(REDIS_KEY_PREFIX + "longUrl:" +longUrl, new TypeReference<String>() {});
        if(shortKey != null) {
            return shortKey;
        }
        // 查询数据库是否已有相同 longUrl      Redis 会过期
        ShortUrl existing = shortUrlRepository.findByLongUrl(longUrl);
        if (existing != null) {
            // 存 Redis 1天期限 防止重复创建
            shortKey = existing.getShortKey();
            redisUtil.setObjectByKey(REDIS_KEY_PREFIX + "longUrl:" + longUrl, shortKey, 1, TimeUnit.DAYS);
            return existing.getShortKey(); // 直接返回已有短链接
        }

        // 1. 生成短链接 Key (BASE62 + 雪花算法)
        shortKey = ShortKeyGenerator.generate();

        // 2. 存入数据库
        ShortUrl entity = ShortUrl.builder()
                .shortKey(shortKey)
                .longUrl(longUrl)
                .build();
        shortUrlRepository.save(entity);

        // 3. 存入 Redis 缓存
        redisUtil.setObjectByKey(REDIS_KEY_PREFIX + shortKey, longUrl, 7, TimeUnit.DAYS);

        return shortKey;
    }


    public Map<String, Integer> getAccessCount(String shortKey) {
        Map<String, Integer> result = new HashMap<>();

        // 查 Redis
        String redisCount = redisUtil.getTokenByKey(REDIS_HITS_PREFIX + shortKey);
        int redisValue = redisCount != null ? Integer.parseInt(redisCount) : 0;
        result.put("redis",redisValue);

        // 查 数据库
        int mysqlValue = shortUrlRepository.findByShortKey(shortKey)
                .map(ShortUrl::getAccessCount)
                .orElse(0);
        result.put("mysql", mysqlValue);

        return result;
    }
}
