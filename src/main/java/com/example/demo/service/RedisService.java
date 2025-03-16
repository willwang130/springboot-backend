package com.example.demo.service;

import com.example.demo.util.RedisLockUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.example.demo.util.RedisUtil;

import java.sql.Time;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisUtil redisUtil;
    private  final RedisLockUtil redisLockUtil;


    public <T> T getFromCacheWithType(String cacheKey, TypeReference<T> valueTypeRef) {
        return redisUtil.getObjectByKeyAndConvert(cacheKey, valueTypeRef);
    }

    public void setFromCacheWithObject(String cacheKey, Object value, long ttl, TimeUnit unit) {
        redisUtil.setObjectByKey(cacheKey, value, ttl, unit);
    }

    public void deleteFromCache(String cacheKey) {
        redisUtil.deleteCache(cacheKey);
    }

    // Redis tryLock & unlock
    public boolean tryLock(String key, String value, long ttl) {
        return redisLockUtil.tryLock(key, value, ttl);
    }

    public void unlock(String key, String value) {
        redisLockUtil.unlock(key, value);
    }

    public String getStringWithKey(String key) {
        return redisUtil.getTokenByKey(key);
    }

    public List<String> popFromRedisList(String shortUrlBufferKey, int length) {
        return redisUtil.popFromList(shortUrlBufferKey, length);
    }

    public void incrementByInteger(String key, Integer i) {
        redisUtil.incrementBy(key, i);
    }

    public Set<String> getKeysByPattern(String pattern) {
        return redisUtil.getByPattern(pattern);
    }

    public void setKeyExpire(String key, long ttl, TimeUnit unit) {
        redisUtil.setExpire(key, ttl, unit);
    }

    public void invalidTokenToBlackList(String token, long expiration) {
        redisUtil.invalidToken(token, expiration);
    }

    public boolean isTokenBlacklisted(String token) {
        return redisUtil.isTokenBlacklisted(token);
    }

    public boolean isKeyExists(String key) {
        return redisUtil.isExists(key);
    }

    public long getBufferListSize(String bufferKey) {
        return redisUtil.getListSize(bufferKey);
    }

    public long getTotalHitsCount() {
        return redisUtil.getTotalHits();
    }







}
