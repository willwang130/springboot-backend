package com.example.demo.util;

import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.*;


@Slf4j
@Component
@RequiredArgsConstructor
public class BloomFilterUtil {
    private final RedissonClient redissonClient;
    private final ProductRepository productRepository;
    private final ShortUrlRepository shortUrlRepository;
    private static final String BLOOM_FILTER_NAME_PRODUCT = "bloom:product:";
    private static final String BLOOM_FILTER_NAME_SHORT = "bloom:shortUrl:";

    // 初始化过滤器
    public void initBloomFilter(String filterName, long expectedInsertions, double falseProbability) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(filterName);
        bloomFilter.tryInit(expectedInsertions, falseProbability);
    }

    // 添加 JSON 数据到 Bloom 过滤器
    public void addToBloomFilter(String filterName, String value) {
        try {
            RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(filterName);

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
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(filterName);
        return bloomFilter.contains(value);
    }

    // 每天凌晨 3点 执行重建 Bloom
    //@Scheduled(cron = "0 0 3 * * ?")
    public ResponseEntity<ApiResponseDTO<Map<String, Integer>>> resetBloomFilter() {
        log.info("正在重建 Bloom Filters...");

        // 1. 找到过滤器
        RBloomFilter<String> bloomFilter1 = redissonClient.getBloomFilter(BLOOM_FILTER_NAME_PRODUCT);
        RBloomFilter<String> bloomFilter2 = redissonClient.getBloomFilter(BLOOM_FILTER_NAME_SHORT);

        // 2. 删除过滤器
        bloomFilter1.delete();
        bloomFilter2.delete();

        // 3. 初始化过滤器
        bloomFilter1.tryInit(10000, 0.01);
        bloomFilter2.tryInit(10000, 0.01);

        // 4. 重新加载数据库数据 Product
        List<Long> productsIds = productRepository.findAllProductIds();
        productsIds.forEach(id -> bloomFilter1.add(id.toString()));

        // 4. 重新加载数据库数据 ShorUrl
        List<Long> shortUrlIds = shortUrlRepository.findAllShortUrlIds();
        shortUrlIds.forEach(id -> bloomFilter2.add(id.toString()));

        // 5. 记录日志
        Map<String, Integer> result = new HashMap<>();
        result.put("products", productsIds.size());
        result.put("shortUrls", shortUrlIds.size());

        log.info("Bloom Filter 重建完成: Product 数据 {} 条, ShortUrl 数据 {} 条", productsIds.size(), shortUrlIds.size());
        return ResponseEntity.ok(new ApiResponseDTO<>(200, "Bloom Filters 重建完成", result));
    }
}
