package com.example.demo.service;

import com.example.demo.entity.Product;
import com.example.demo.dto.ProductDTO;
import com.example.demo.repository.ProductRepository;
import com.example.demo.exception.ProductNotFoundException;
import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.util.BloomFilterUtil;
import com.example.demo.webSocket.WebSocketNotificationHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository; // ProductRepository是数据库操作的接口, 用于增删改查Product表的数据
    private final RedisService redisService;
    private final BloomFilterUtil bloomFilterUtil;
    private final WebSocketNotificationHandler notificationHandler;
    private static final String BLOOM_FILTER_NAME_PRODUCT = "bloom:product:";

    public ResponseEntity<ApiResponseDTO<ProductDTO>> getProductById(Long id) {
        String cacheKey = "product:" + id;

        // 1. 先查 Bloom 过滤器
        if (!bloomFilterUtil.mightContain(BLOOM_FILTER_NAME_PRODUCT, id.toString())) {
            return ResponseEntity.status(404)
                    .body(new ApiResponseDTO<>(404, "产品 ID " + id + "不存在", null));
        };

        // 2. 查询缓存
        ProductDTO cacheProduct = redisService.getFromCacheWithType(cacheKey, new TypeReference<ProductDTO>() {});
        if (cacheProduct != null) { // 防穿透
            if("NULL".equals(cacheProduct.getName())) {
                log.warn("产品 ID {} 在 Redis 缓存中标记为空数据，返回 404", id);
                return ResponseEntity.status(404)
                        .body(new ApiResponseDTO<>(404, "产品 ID " + id + "不存在", null));
            }
            log.warn("从 Redis 缓存中获取产品: cacheProduct:ID={} name={}, price={}",id, cacheProduct.getName(), cacheProduct.getPrice());
            return ResponseEntity.ok(new ApiResponseDTO<>(200, "成功", cacheProduct));
        }

        // 3. 查询 MySQL
        Product product =  productRepository.findById(id)
                .orElse(null);
        if (product == null) { // 如果不存在, 缓存空值防止缓存穿透
            log.warn("产品 ID {} 不存在，缓存空值以防止缓存穿透", id);
            redisService.setFromCacheWithObject(cacheKey, new ProductDTO(null, "NULL", 0.0), 60, TimeUnit.SECONDS);
            return  ResponseEntity.status(404).body(new ApiResponseDTO<>(404, "产品 ID " + id + " 不存在",null));
        }

        // 将数据库数据转换为 DTO，并存入 Redis
        ProductDTO productDTO = new ProductDTO(product.getId(), product.getName(), product.getPrice());
        redisService.setFromCacheWithObject(cacheKey, productDTO, 10, TimeUnit.MINUTES);

        return ResponseEntity.ok(new ApiResponseDTO<>(200, "成功", productDTO));
    }

    public ResponseEntity<ApiResponseDTO<List<ProductDTO>>> getAllProducts() {

        String cacheKey = "all_products";
        // 1. 查询缓存
        List<ProductDTO> cacheProducts = redisService.getFromCacheWithType(cacheKey, new TypeReference<List<ProductDTO>>() {});

        if (cacheProducts != null) {
            return ResponseEntity.ok(new ApiResponseDTO<>(200, "成功", cacheProducts));
        }

        // 2. 查询 MySQL
        List<Product> products = productRepository.findAll();

        if (products.isEmpty()) {
            log.warn("No products found in database when finding all products, returning empty list.");
            // 防止穿透
            redisService.setFromCacheWithObject(cacheKey, Collections.emptyList(), 30, TimeUnit.SECONDS);
            return ResponseEntity.status(404)
                    .body(new ApiResponseDTO<>(404, "MySQL 没有", null));
        }

        // 将查询到的 Product 转换成 ProductDTO 列表
        List<ProductDTO> productDTOS = products.stream()
                .map(product -> new ProductDTO(product.getId(), product.getName(), product.getPrice()))
                .collect(toList());

        // 存入 cache 时限 10分钟
        redisService.setFromCacheWithObject(cacheKey, productDTOS, 3, TimeUnit.MINUTES);

        return ResponseEntity.ok(new ApiResponseDTO<>(200, "成功", productDTOS));
    }

    public ResponseEntity<ApiResponseDTO<List<ProductDTO>>> getProductByCategoryAndMinPrice(String category, double minPrice) {

        String cacheKey = "category:" + category + " minPrice:" + minPrice;

        // 查 Redis
        List<ProductDTO> cacheProducts = redisService.getFromCacheWithType(cacheKey, new TypeReference<List<ProductDTO>>() {});
        if (cacheProducts != null) {
            return ResponseEntity.ok(new ApiResponseDTO<>(200, "成功", cacheProducts));
        }

        // 查 MySQL
        List<Product> products = productRepository.findByNameContainingAndPriceGreaterThanEqual(category, minPrice);
        if (products.isEmpty()) {
            log.warn("No products found in database with category {} minPrice {}, returning empty list.", category, minPrice);
            // 防止穿透
            redisService.setFromCacheWithObject(cacheKey, Collections.emptyList(), 30, TimeUnit.SECONDS);
            return ResponseEntity.status(404)
                    .body(new ApiResponseDTO<>(404, "MySQL返回: 没有", null));
        }

        // to DTOs 再存入 Redis
        List<ProductDTO> productDTOS = products.stream()
                .map(product -> new ProductDTO(product.getId(), product.getName(), product.getPrice()))
                .collect(toList());

        redisService.setFromCacheWithObject(cacheKey, productDTOS, 3, TimeUnit.MINUTES);

        return ResponseEntity.ok(new ApiResponseDTO<>(200, "成功", cacheProducts));
    }


    public ResponseEntity<ApiResponseDTO<ProductDTO>> addProduct(ProductDTO productDTO) {
        String lockKey = "lock:add_product:";
        String requestId = UUID.randomUUID().toString();
        log.info("Enter ProductService ok");

        // 尝试获取 Redis 分布式锁，最多等待 10 秒
        if (!redisService.tryLock(lockKey, requestId, 10)) {
            log.info("请求过于繁忙, 请稍后再试");
            return ResponseEntity.status(429)
                    .body(new ApiResponseDTO<>(429,"请求过于繁忙, 请稍后再试, Server is busy, try later", null));
        }

        log.info("Set Lock ok");
        try {
            Product product = Product.builder()
                    .name(productDTO.getName())
                    .price(productDTO.getPrice())
                    .build();

            productRepository.save(product);
            productDTO.setId(product.getId()); // 设置 ID

            redisService.setFromCacheWithObject("product:" + product.getId(), productDTO, 10, TimeUnit.MINUTES);
            // 加入 Bloom 过滤器
            bloomFilterUtil.addToBloomFilter(BLOOM_FILTER_NAME_PRODUCT, product.getId().toString());

            // 考虑之后将其分片缓存或采取分布式缓存管理方案（例如 Redis Cluster 或分布式缓存工具如 Hazelcast）。
            log.info("调用 redisService.deleteCache(\"all_products\")");
            redisService.deleteFromCache("all_products");

            // 新增产品后通知 WebSocket
            notificationHandler.sendNotification("新增产品: " + productDTO.getName());

            return ResponseEntity.status(201).body(new ApiResponseDTO<>(201, "产品创建成功", productDTO));

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            redisService.unlock(lockKey, requestId);   // 释放锁
        }
    }

    @Transactional
    public ResponseEntity<ApiResponseDTO<Long>> updateProduct(Long id, ProductDTO productDTO) {
        String lockKey = "lock:update_product:" + id;
        String requestId  = UUID.randomUUID().toString();

        if (!redisService.tryLock(lockKey, requestId , 10)) {
            return ResponseEntity.status(429)
                    .body(new ApiResponseDTO<>(429, "请求过于繁忙，请稍后再试", null));
        }

        try {
            // 查询产品 查不到抛 404 异常
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new ProductNotFoundException("Product ID " + id + " not found for updating"));

            // 更新数据库产品信息
            product.setName(productDTO.getName());
            product.setPrice(productDTO.getPrice());
            productRepository.save(product);

            // 更新 Redis 缓存
            redisService.setFromCacheWithObject("product:" + id, productDTO, 10, TimeUnit.MINUTES);
            // 加入 Bloom 过滤器
            bloomFilterUtil.addToBloomFilter(BLOOM_FILTER_NAME_PRODUCT, product.getId().toString());

            // 考虑之后将其分片缓存或采取分布式缓存管理方案（例如 Redis Cluster 或分布式缓存工具如 Hazelcast）。
            redisService.deleteFromCache("all_products");

            log.debug("Product ID {} successfully updated", id);

            // 更新产品后通知 WebSocket
            notificationHandler.sendNotification("产品更新: " + productDTO.getName() + " ID: " + id);

            return ResponseEntity.status(201)
                    .body(new ApiResponseDTO<>(200, "产品更新成功", id));

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            log.info("Releasing lock: {}", lockKey);
            redisService.unlock(lockKey, requestId);
        }
    }

    public ResponseEntity<ApiResponseDTO<Long>> deleteProduct(Long id) {
        String lockKey = "lock:delete_product:" + id;
        String requiredId = UUID.randomUUID().toString();

        if (!redisService.tryLock(lockKey, requiredId, 10)) {
            return ResponseEntity.status(429) // 429 Too Many Requests
                    .body(new ApiResponseDTO<>(429, "请求过于繁忙，请稍后再试", null));
        }
        try {
            if (!productRepository.existsById(id)) {
                return ResponseEntity.status(404)
                        .body(new ApiResponseDTO<>(404, "产品 ID " + id + " 不存在, 无法删除", null));
            }

            productRepository.deleteById(id);

            // 缓存 NULL 减少误判
            redisService.setFromCacheWithObject(
                    "product:" + id, new ProductDTO(null, "NULL", 0.0), 60, TimeUnit.SECONDS);
            // 考虑之后将其分片缓存或采取分布式缓存管理方案（例如 Redis Cluster 或分布式缓存工具如 Hazelcast）。
            redisService.deleteFromCache("all_products");


            // 更新产品后通知 WebSocket
            notificationHandler.sendNotification("产品删除成功 ID: " + id);


            return ResponseEntity.ok(new ApiResponseDTO<>(200, "产品删除成功", id));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            redisService.unlock(lockKey, requiredId);
        }

    }

}
