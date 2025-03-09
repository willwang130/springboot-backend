package com.example.demo.service;

import com.example.demo.entity.Product;
import com.example.demo.dto.ProductDTO;
import com.example.demo.exception.ProductLockException;
import com.example.demo.repository.ProductRepository;
import com.example.demo.exception.ProductNotFoundException;
import com.example.demo.dto.ApiResponseDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    public ProductDTO getProductById(Long id) {
        String cacheKey = "product:" + id;

        // 先查询缓存
        ProductDTO cacheProduct = redisService.getAndConvertValueToType(cacheKey, new TypeReference<ProductDTO>() {});
        if (cacheProduct != null) {
            if("NULL".equals(cacheProduct.getName())) {
                log.warn("产品 ID {} 在 Redis 缓存中标记为空数据，返回 404", id);
                throw new ProductLockException("产品 ID " + id + " 不存在");
            }
            log.warn("从 Redis 缓存中获取产品: cacheProduct:ID={} name={}, price={}",id, cacheProduct.getName(), cacheProduct.getPrice());
             return cacheProduct;
        }

        // 查询数据库，如果不存在，缓存空值防止缓存穿透
        Product product =  productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("产品 ID {} 不存在，缓存空值以防止缓存穿透", id);
                    redisService.setCache(cacheKey, new ProductDTO(null, "NULL", 0.0), 60, TimeUnit.SECONDS);
                    return new ProductLockException("产品 ID " + id + " 不存在");
                });

        // 将数据库数据转换为 DTO，并存入缓存
        ProductDTO productDTO = new ProductDTO(product.getId(), product.getName(), product.getPrice());
        redisService.setCache(cacheKey, productDTO, 10, TimeUnit.MINUTES);
        return productDTO;
    }

    public List<ProductDTO> getAllProducts() {

        String cacheKey = "all_products";
        // 从 Redis 获取缓存的所有产品数据
        List<ProductDTO> cacheProducts = redisService.getAndConvertValueToType(cacheKey, new TypeReference<List<ProductDTO>>() {});

        // cache 有的话直接返回
        if (cacheProducts != null) {
            return cacheProducts;
        }

        // 如果缓存没有 查询数据库
        List<Product> products = productRepository.findAll();

        if (products.isEmpty()) {
            log.warn("No products found in database when finding all products, returning empty list.");
            // 防止穿透
            redisService.setCache(cacheKey, Collections.emptyList(), 30, TimeUnit.SECONDS);
            return Collections.emptyList();
        }

        // 将查询到的 Product 转换成 ProductDTO 列表
        List<ProductDTO> productDTOS = products.stream()
                .map(product -> new ProductDTO(product.getId(), product.getName(), product.getPrice()))
                .collect(toList());

        // 存入 cache 时限 10分钟
        redisService.setCache(cacheKey, productDTOS, 3, TimeUnit.MINUTES);

        return productDTOS;
    }

    public List<ProductDTO> getProductByCategoryAndMinPrice(String category, double minPrice) {

        String cacheKey = "category:" + category + " minPrice:" + minPrice;

        List<ProductDTO> cacheProducts = redisService.getAndConvertValueToType(cacheKey, new TypeReference<List<ProductDTO>>() {});

        if (cacheProducts != null) {
            return cacheProducts;
        }

        List<Product> products = productRepository.findByNameContainingAndPriceGreaterThanEqual(category, minPrice);

        if (products.isEmpty()) {
            log.warn("No products found in database with category {} minPrice {}, returning empty list.", category, minPrice);
            // 防止穿透
            redisService.setCache(cacheKey, Collections.emptyList(), 30, TimeUnit.SECONDS);
            return Collections.emptyList();
        }

        // to DTOS
        List<ProductDTO> productDTOS = products.stream()
                .map(product -> new ProductDTO(product.getId(), product.getName(), product.getPrice()))
                .collect(toList());

        redisService.setCache(cacheKey, productDTOS, 3, TimeUnit.MINUTES);

        return productDTOS;
    }


    public ProductDTO addProduct(ProductDTO productDTO) {
        String lockKey = "lock:add_product";
        String requestId = UUID.randomUUID().toString();
        log.info("Enter ProductService ok");

        // 尝试获取 Redis 分布式锁，最多等待 10 秒
        if (!redisService.tryLock(lockKey, requestId, 10)) {
            throw new ProductLockException("请求过于繁忙, 请稍后再试, Server is busy, try later");
        }
        log.info("Set Lock ok");
        try {
            Product product = Product.builder()
                    .name(productDTO.getName())
                    .price(productDTO.getPrice())
                    .build();

            productRepository.save(product);
            redisService.setCache("product:" + product.getId(), productDTO, 10, TimeUnit.MINUTES);
            // 考虑之后将其分片缓存或采取分布式缓存管理方案（例如 Redis Cluster 或分布式缓存工具如 Hazelcast）。

            log.info("调用 redisService.deleteCache(\"all_products\")");
            redisService.deleteCache("all_products");

            return productDTO;

        } finally {
            redisService.unlock(lockKey, requestId);   // 释放锁
        }
    }

    @Transactional
    public long updateProduct(Long id, ProductDTO productDTO) {
        String lockKey = "lock:update_product" + id;
        String requestId  = UUID.randomUUID().toString();

        if (!redisService.tryLock(lockKey, requestId , 10)) {
            throw new ProductLockException("请求过于繁忙, 请稍后再试, 已被锁");
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
            redisService.setCache("product:" + id, productDTO, 10, TimeUnit.MINUTES);
            // 考虑之后将其分片缓存或采取分布式缓存管理方案（例如 Redis Cluster 或分布式缓存工具如 Hazelcast）。
            redisService.deleteCache("all_products");

            log.debug("Product ID {} successfully updated", id);

            return id;
        } finally {
            log.info("Releasing lock: {}", lockKey);
            redisService.unlock(lockKey, requestId);
        }
    }

    public ResponseEntity<ApiResponseDTO<Long>> deleteProduct(Long id) {
        String lockKey = "lock:delete_product" + id;
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

            redisService.deleteCache("product:" +id);
            // 考虑之后将其分片缓存或采取分布式缓存管理方案（例如 Redis Cluster 或分布式缓存工具如 Hazelcast）。
            redisService.deleteCache("all_products");

            return ResponseEntity.ok(new ApiResponseDTO<>(200, "产品删除成功", id));
        } finally {
            redisService.unlock(lockKey, requiredId);
        }

    }

}
