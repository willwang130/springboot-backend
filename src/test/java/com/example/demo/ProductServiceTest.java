package com.example.demo;

import com.example.demo.dto.ProductDTO;
import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.ProductService;
import com.example.demo.service.RedisService;
import com.example.demo.util.BloomFilterUtil;
import com.example.demo.util.RedisUtil;
import com.example.demo.webSocket.WebSocketNotificationHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@Disabled
@Slf4j
@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private RedisService redisService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productRepository, redisService, mock(BloomFilterUtil.class), mock(WebSocketNotificationHandler.class));
    }


    @Test
    void testConcurrentAddProduct() throws InterruptedException {
        int threadCount = 10; // 并发线程数
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // 确保只有一个线程成功获取锁
        AtomicBoolean lockAcquired = new AtomicBoolean(false);
        when(redisService.tryLock(anyString(), anyString(), anyLong()))
                .thenAnswer(invocation -> lockAcquired.compareAndSet(false, true));

        // Mock ProductRepository.save()，确保返回非空 ID
        when(productRepository.save(any())).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(1L); // 确保 ID 不为空
            return product;
        });

        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    ProductDTO productDTO = new ProductDTO(1L, "Concurrent Product", 50.0);
                    productService.addProduct(productDTO); // 多线程并发执行
                } catch (Exception e) {
                    log.info("Test: addProduct() failed: {}", e.getMessage());
                }finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();
        executorService.shutdown();

        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
            log.warn(" 强制关闭 ExecutorService");
            executorService.shutdownNow(); // 超时后强制关闭
        }

        // 确保 Redis 锁被调用
        verify(redisService, atLeastOnce()).tryLock(anyString(),anyString(),anyLong());
        verify(productRepository, atMost(1)).save(any()); // 最多 1 次
    }
}
