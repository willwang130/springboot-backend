package com.example.demo;

import com.example.demo.config.RedisConfig;
import com.example.demo.service.RedisService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.sql.Time;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
@SpringBootTest(properties = {
        "spring.redis.host=backend-redis",  // Docker 内的 `backend-redis`
        "spring.redis.port=6379",
        "spring.redis.port=6379",
        "spring.redis.password="
})
@ContextConfiguration(classes = RedisConfig.class)
public class RedisTest {

    @Autowired
    private RedisService redisService;

    @Test
    void testRedisStringStorage() {
        redisService.setFromCacheWithObject("test:string:", "Hello Redis", 10, TimeUnit.MINUTES);
        String value = redisService.getFromCacheWithType("test:string:", new TypeReference<String>() {});
        assertEquals("Hello Redis", value);
    }

    @Test
    void testRedisLock() {
        boolean locked = redisService.tryLock("lock:test", "123", 5);
        assertTrue(locked);

        redisService.unlock("lock:test", "123");

        boolean locked2 = redisService.tryLock("lock:test", "123", 5);
        redisService.unlock("lock:test", "123");
    }
}
