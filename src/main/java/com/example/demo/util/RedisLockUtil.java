package com.example.demo.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisLockUtil {


    private final RedisUtil redisUtil;

    public RedisLockUtil(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    // 获取 Redis 分布式锁
    public boolean tryLock(String key, String value, long timeout) {
        // 获得线程id
        //long threadId = Thread.currentThread().getId();
        log.info("Enter LockUtil");
        try {
            boolean success = redisUtil.setIfLock(key, value, timeout);
            log.info("[tryLock] key={}, success={}",key, success);
            return success;

        } catch (Exception e) {
            log.info("[tryLock] Redis 操作失败：{}", e.getMessage());
            return false;
        }
    }

    public void unlock(String key, String value) {
        log.info("Enter [unlock] key={}", key);
        String currentValue = redisUtil.getTokenByKey(key);
        if (value.equals(currentValue)) {
            redisUtil.deleteCache(key);
            log.info("Did [unlock] key={}", key);
        }
    }



//    // 获取 Redis 分布式锁（使用 Lua 确保原子性）
//    public boolean tryLock(String key, String value, long timeout) {
//        String luaScript =
//                "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then " +
//                        "   redis.call('expire', KEYS[1], ARGV[2]) " +
//                        "   return 1 " +
//                        "else " +
//                        "   return 0 " +
//                        "end";
//        RedisScript<Long> redisScript = RedisScript.of(luaScript, Long.class);
//        Long result = stringRedisTemplate.execute(redisScript, Collections.singletonList(key), value, String.valueOf(timeout));
//
//        boolean success = (result != null && result == 1);
//        log.info("@@@@@@@@ tryLock() - key: {}, success: {}",key, success);
//        return success;
//    }

    //    // 释放锁（确保只有自己能释放）
//    public void unlock(String key, String value) {
//        String luaScript =
//                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
//                        "   return redis.call('del', KEYS[1]) " +
//                        "else " +
//                        "   return 0 " +
//                        "end";
//        RedisScript<Long> redisScript = RedisScript.of(luaScript, Long.class);
//        Long result = stringRedisTemplate.execute(redisScript, Collections.singletonList(key), value);
//
//        log.info("@@@@@@@@@@@ unlock() - key: {}, result: {}", key, result);
//    }

}
