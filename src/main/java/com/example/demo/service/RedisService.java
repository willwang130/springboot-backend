package com.example.demo.service;

import com.example.demo.util.RedisLockUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.example.demo.util.RedisUtil;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisUtil redisUtil;
    private  final RedisLockUtil redisLockUtil;


    public <T> T getAndConvertValueToType(String cacheKey, TypeReference<T> valueTypeRef) {

        return redisUtil.getObjectByKeyAndConvert(cacheKey, valueTypeRef);
    }

    public void setCache(String cacheKey, Object value, long ttl, TimeUnit unit) {
        redisUtil.setObjectByKey(cacheKey, value, ttl, unit);
    }

    public void deleteCache(String cacheKey) {
        redisUtil.deleteCache(cacheKey);
    }

    // Redis tryLock & unlock
    public boolean tryLock(String key, String value, long ttl) {
        log.info("Enter redisService");
        return redisLockUtil.tryLock(key, value, ttl);
    }

    public void unlock(String key, String value) {
        redisLockUtil.unlock(key, value);
    }

}
