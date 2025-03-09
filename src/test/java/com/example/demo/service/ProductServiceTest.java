package com.example.demo.service;

import com.example.demo.config.TestConfig;
import com.example.demo.dto.ProductDTO;
import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;
import com.example.demo.util.RedisLockUtil;
import com.example.demo.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@Disabled
@Slf4j
public class ProductServiceTest {

    @InjectMocks
    private ProductService productService; // 只允许一个 @InjectMocks

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RedisUtil redisUtil;  // 作为 Mock，避免真实 Redis 操作

    private RedisService redisService;  // 需要手动创建这个实例

//    @BeforeEach
//    void setUp() {
//        // 我们手动初始化
//        RedisLockUtil redisLockUtil = new RedisLockUtil(redisUtil);
//        // 然后手动注入到 ProductService
//        redisService = new RedisService(redisUtil, redisLockUtil);
//        productService = new ProductService(productRepository, redisService);
//    } @BeforeEach
//    void setUp() {
//        // 我们手动初始化
//        RedisLockUtil redisLockUtil = new RedisLockUtil(redisUtil);
//        // 然后手动注入到 ProductService
//        redisService = new RedisService(redisUtil, redisLockUtil);
//        productService = new ProductService(productRepository, redisService);
//    }
//
//    @InjectMocks
//    private ProductService productService;
//
//    @Mock
//    private ProductRepository productRepository;
//
//    @InjectMocks
//    private RedisService redisService;
//
//    @InjectMocks
//    private RedisLockUtil redisLockUtil;
//
//    @Mock
//    private RedisUtil redisUtil;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);  // ✅ 初始化所有 @Mock 注解的对象
//    }
    @Disabled
    @Test
    public void testAddProduct_LockAcquired() {
        // 1 模拟 tryLock 成功
        when(redisService.tryLock(anyString(), anyString(), anyLong()))
                .thenAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    String value = invocation.getArgument(1);
                    Long ttl = invocation.getArgument(2);
                    System.out.println("A. Mocked tryLock called with key=" + key + ", value=" + value + ", ttl=" + ttl);
                    return true;
                });

        // 2 模拟数据库保存
        ProductDTO productDTO = new ProductDTO(1L,"Laptop", 2000.0);
        Product mockProduct = Product.builder().id(1L).name("Laptop").price(2000.0).build();
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);

        // 3 执行方法    确保不会抛出 ProductLockException
        assertDoesNotThrow(() -> productService.addProduct(productDTO));

        // 4 断言：验证锁成功，save 被调用
        verify(productRepository, times(1)).save(any(Product.class));
//        verify(redisService, times(1)).deleteCache("all_products");
//
//        // 5 断言：确保 unlock() 被调用
//        verify(redisLockUtil, times(1)).unlock(anyString(), anyString());
    }
    @Disabled
    @Test
    void testConcurrentAddProduct() throws InterruptedException {
        int threadCount = 10; // 并发线程数
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        when(redisUtil.setIfLock(anyString(), anyString(), anyLong())).thenReturn(true);

        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    ProductDTO productDTO = new ProductDTO(1L, "Concurrent Product", 50.0);
                    productService.addProduct(productDTO); // 多线程并发执行
                    log.info("B.  addProduct() success");
                } catch (Exception e) {
                    log.info("B.  addProduct() failed: {}", e.getMessage());
                }finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await(); // 等待所有线程执行完成
        executorService.shutdown();

        // 确保 Redis 锁被调用
        verify(redisUtil, atLeastOnce()).setIfLock(anyString(),anyString(),anyLong());
        verify(productRepository, atMost(1)).save(any()); // 最多 1 次
    }
}
