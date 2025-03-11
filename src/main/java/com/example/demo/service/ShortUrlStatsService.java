package com.example.demo.service;

import com.example.demo.repository.ShortUrlRepository;
import com.example.demo.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortUrlStatsService {

    private final RedisUtil redisUtil;
    private final ShortUrlRepository shortUrlRepository;
    private static final String REDIS_HITS_PREFIX = "short_url_hits:";

    // 每 5 秒执行一次 Redis List 更新到 Redis
    @Transactional
    @Scheduled(fixedRate = 5 * 1000)
    public void synAccessCountToRedis() {
        String redisListKey = "short_url_buffer";
        while (true) {
            // 一次取 100 个, 避免超大流量
            List<String> shortKeyList = redisUtil.popFromRedisList("short_url_buffer", 100);
            if (shortKeyList == null || shortKeyList.isEmpty()) {
                break;
            }

            // 统计每个点链接的访问次数
            Map<String, Integer> countMap = new HashMap<>();
            for (String shortKey : shortKeyList) {
                countMap.put(shortKey, countMap.getOrDefault(shortKey, 0) + 1);
            }

            // 统一更新给 Redis 访问次数
            for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
                redisUtil.incrementByInteger(REDIS_HITS_PREFIX + entry.getKey(), entry.getValue());
                redisUtil.incrementByInteger("total_hits_count:", entry.getValue());  // 记录总访问量 测试用
            }
        }
    }

    // 每 90 秒执行一次 Redis 更新到 数据库
    @Transactional
    @Scheduled(fixedRate = 90  * 1000)
    public void synAccessCountToDatabase() throws InterruptedException {

        // 获取所有 Redis 计数 Key
        Set<String> keys = redisUtil.getKeysByPattern(REDIS_HITS_PREFIX + "*");

        for (String redisKey : keys) {
            String shortKey = redisKey.replace(REDIS_HITS_PREFIX, ""); // 获取短链接 Key
            String redisCount = redisUtil.getTokenByKey(redisKey);

            if (redisCount != null) {
                int count = Integer.parseInt(redisCount);
                log.info("更新访问计数");
                shortUrlRepository.incrementAccessCount(shortKey, count);

                // 清空 Redis 计数
                redisUtil.deleteCache(redisKey);
            }
        }
    }
}
