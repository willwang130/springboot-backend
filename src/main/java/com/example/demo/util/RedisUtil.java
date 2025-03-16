package com.example.demo.util;

import com.example.demo.exception.CacheConversionException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;


    public <T> T getObjectByKeyAndConvert(String key, TypeReference<T> valueTypeRef) {
        String cacheData = stringRedisTemplate.opsForValue().get(key);

        if (cacheData == null) return null;

        try {
            // JSON String 转为 valueTypeRef 类型 再返回
            return objectMapper.readValue(cacheData, valueTypeRef);
        } catch (Exception e) {
            throw new CacheConversionException(
                    "Redis conversion failed for type " + valueTypeRef.getType().getTypeName(), e);
        }
    }

    public void setObjectByKey(String key, Object value, long ttl, TimeUnit unit) {
        try {
            // Object 转为 JSON String 再存
            String jsonValue = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, jsonValue, ttl, unit);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing value for Redis key: " + key, e);
        }
    }


    public void deleteCache(String key) {
        stringRedisTemplate.delete(key);
    }

    // 没上锁就上锁
    public boolean setIfLock(String key, String value, long ttl) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, value, ttl, TimeUnit.SECONDS));
    }

    // Key 拿 Value
    public String getTokenByKey(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    // 短链接计数器 增加 1 次
    public void increment(String key) {
        stringRedisTemplate.opsForValue().increment(key);
    }

    // 短链接计数器 增加 n 次
    public void incrementBy(String key, Integer n) {
        stringRedisTemplate.opsForValue().increment(key, n);
    }

    // SCAN 查询
    public Set<String> getByPattern(String pattern) {

        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();

        try (Cursor<byte[]> cursor = stringRedisTemplate.execute(
                connection -> connection
                        .keyCommands()
                        .scan(options), true)) {
            // true：使用pipeline提高吞吐量 Scan提交多次还是一次命令到Redis
            while (cursor != null && cursor.hasNext()) {
                keys.add(new String(cursor.next()));
            }
        } catch (Exception e) {
            log.error("Redis SCAN 出错: {}", e.getMessage());
        }

        return keys;
    }

    // Redis List Push 操作
    public void pushToRedisListBuffer(String shortUrlBufferName, String shortKey) {
        stringRedisTemplate.opsForList().rightPush(shortUrlBufferName, shortKey);
    }

    // Redis List Pop 操作
    public List<String> popFromList(String shortBufferKey, int length) {
        List<String> results = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            String tempValue = stringRedisTemplate.opsForList().leftPop(shortBufferKey);
            if (tempValue == null) {
                break;
            }
            results.add(tempValue);
        }
        return results;
    }

    public void setExpire(String key, long ttl, TimeUnit unit) {
        stringRedisTemplate.expire(key, ttl, unit);
    }

    public boolean isExists(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    public long getListSize(String shortUrlBuffer) {
        Long size = stringRedisTemplate.opsForList().size(shortUrlBuffer);
        return size != null ? size : 0;
    }

    public long getTotalHits() {
        String value = stringRedisTemplate.opsForValue().get("total_hits_count:");
        return (value != null && value.matches("\\d+")) ? Long.parseLong(value) : 0;
    }

    public void invalidToken(String token, long expirationMillis) {
        stringRedisTemplate.opsForValue().set("blacklist:" + token, "invalid", expirationMillis, TimeUnit.MILLISECONDS);
    }

    public boolean isTokenBlacklisted(String token) {
        return stringRedisTemplate.opsForValue().get("blacklist:" + token) != null;
    }


    public String testRedisConnection() {
        try {
            System.out.println("正在尝试连接 Redis...");
            stringRedisTemplate.opsForValue().set("test_connection", "success", 10, TimeUnit.SECONDS);
            String value = stringRedisTemplate.opsForValue().get("test_connection");
            System.out.println("Redis 连接成功: " + value);
            return "Redis 连接成功: " + value;
        } catch (Exception e) {
            System.out.println("Redis 连接失败：" + e.getMessage());
            return "Redis 连接失败：" + e.getMessage();
        }
    }
}
