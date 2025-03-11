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
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;


    public <T> T getObjectByKeyAndConvert(String key, TypeReference<T> valueTypeRef) {
        Object cacheData = redisTemplate.opsForValue().get(key);

        return convertToType(cacheData, valueTypeRef);
    }

    public void setObjectByKey(String key, Object value, long ttl, TimeUnit unit) {
        try {
            // 确保存入 Redis 的数据是 JSON
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, ttl, unit);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing value for Redis key: " + key, e);
        }
    }

    public void deleteCache(String key) {
        redisTemplate.delete(key);
    }

    public void setExpire(String key, long ttl, TimeUnit unit) {
        redisTemplate.expire(key, ttl, unit);
    }

    public <T> T convertToType (Object cacheData, TypeReference<T> valueTypeRef) {
        if (cacheData != null) {
            try {
                // 讲存储在 cache 里的 JSON 数据转换成 valueTypeRef 类型
                log.info("Before Conversion name={}, price=[}",cacheData);
                T obj = objectMapper.readValue(cacheData.toString(), valueTypeRef);
                log.info("After Conversion name={}, price=[}",obj.toString());
                return obj;
            } catch (Exception e) {
                throw new CacheConversionException("Redis conversion failed for type " + valueTypeRef.getType().getTypeName(), e);
            }
        }
        return null;
    }

    public boolean setIfLock(String key, String value, long ttl) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, value, ttl, TimeUnit.SECONDS));
    }

    public String getTokenByKey(String key) {
        return stringRedisTemplate.opsForValue().get(key);
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

    public void increment(String key) {
        redisTemplate.opsForValue().increment(key);
    }

    public void incrementByInteger(String key, Integer i) {
        redisTemplate.opsForValue().increment(key, i);
    }

    public Set<String> getKeysByPattern(String pattern) {
        Set<String> keys = new HashSet<>();
        try (Cursor<byte[]> cursor = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection()
                .scan(ScanOptions.scanOptions().match(pattern).count(1000).build())) { // 替换旧 scan 方法
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next()));
            }
        } catch (Exception e) {
            System.err.println("Redis SCAN 出错: " + e.getMessage());
        }
        return keys;  // 确保永远不会返回 null
    }

    public void pushToRedisListBuffer(String shortUrlBufferName, String shortKey) {
        redisTemplate.opsForList().rightPush(shortUrlBufferName, shortKey);
    }

    public List<String> popFromRedisList(String shortUrlBufferKey, int len) {
        List<String> results = new ArrayList<>();
        for(int i = 0; i < len; i++) {
            String tempValue = (String) redisTemplate.opsForList().leftPop(shortUrlBufferKey);
            if(tempValue == null) {
                break;
            }
            results.add(tempValue);
        }
        return results;
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public long getListSize(String shortUrlBuffer) {
        Long size = redisTemplate.opsForList().size(shortUrlBuffer);
        return size != null ? size : 0;
    }

    public long getTotalHitsCount() {
        String value = stringRedisTemplate.opsForValue().get("total_hits_count:");
        return (value != null && value.matches("\\d+")) ? Long.parseLong(value) : 0;
    }

}
