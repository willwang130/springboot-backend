package com.example.demo.service;

import com.example.demo.repository.ShortUrlRepository;
import com.example.demo.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortUrlStatsService {

    private final RedisUtil redisUtil;
    private final ShortUrlRepository shortUrlRepository;
    private static final String REDIS_HITS_PREFIX = "short_url_hits:";


    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void updateAccessCount(String shortKey) {
        log.info("RabbitMQ received message: shortKey = {}", shortKey);

        // 直接更新 Redis 计数
        redisUtil.increment("short_url_hits:" + shortKey);
    }


    // 每 1 分钟执行一次 Redis 更新数据库
    @Transactional
    @Scheduled(fixedRate = 60000)
    public void synAccessCountToDatabase() {

        // 获取所有 Redis 计数 Key
        Set<String> keys = redisUtil.getKeysByPattern(REDIS_HITS_PREFIX + "*");

        for (String redisKey : keys) {
            String shortKey = redisKey.replace(REDIS_HITS_PREFIX, ""); // 获取短链接 Key
            String redisCount = redisUtil.getTokenByKey(redisKey);

            if (redisCount != null && redisCount.matches("\\d+")) {
                int count = Integer.parseInt(redisCount);

                if (count > 0) {
                    // 批量更新数据库
                    log.info(" 更新访问计数: shortKey={}, count={}",shortKey, count);
                    shortUrlRepository.incrementAccessCount(shortKey, count);

                    // 清空 Redis 计数
                    redisUtil.deleteCache(redisKey);
                }
            }
        }
    }


}
