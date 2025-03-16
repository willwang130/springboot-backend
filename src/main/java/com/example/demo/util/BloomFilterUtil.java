package com.example.demo.util;

import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class BloomFilterUtil {
    private final RedissonClient redissonClient;
    private final ProductRepository productRepository;

    // 初始化过滤器
    public void initBloomFilter(String filterName, long expectedInsertions, double falseProbability) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(filterName);
        bloomFilter.tryInit(expectedInsertions, falseProbability);
    }

    // 添加 JSON 数据到 Bloom 过滤器
    public void addToBloomFilter(String filterName, String value) {
        try {
            RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("bloom:" + filterName);

            // 如果 Bloom 第一次运行, 则初始化, 其他时间不再初始化
            if (bloomFilter.isExists()) {
                log.warn("Bloom 过滤器 {} 未初始化. 正在初始化...", filterName);
                bloomFilter.tryInit(10000, 0.01); // 默认 1 万个数据, 误判率 1% 越低占内存越高
            }

            bloomFilter.add(value);
        } catch (Exception e) {
            log.error("Bloom 过滤器添加失败: {}", e.getMessage());
        }

    }

    // 检查数据是否存在
    public boolean mightContain(String filterName, String value) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("bloom:" + filterName);
        return bloomFilter.contains(value);
    }

    // 每天凌晨 3点 执行重建 Bloom
    //@Scheduled(cron = "0 0 3 * * ?")
    public ResponseEntity<ApiResponseDTO<Integer>> resetBloomFilter() {
        log.info("正在重建 Bloom Filter...");

        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("bloom:product:");

        bloomFilter.delete();
        bloomFilter.tryInit(10000, 0.01);

        // 重新加载数据库数据
        List<Long> productsIds = productRepository.findAllProductIds();
        productsIds.forEach(id -> bloomFilter.add(id.toString()));

        int size = productsIds.size();
        log.info("Bloom Filter 重建完成, 添加了 {} 条数据", size);
        return ResponseEntity.ok(new ApiResponseDTO<>(200, "Bloom Filter 重建完成", size));
    }
}
