package com.example.demo.service;

import com.example.demo.dto.ProductDTO;
import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;
import com.example.demo.util.RedisLockUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInstance;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Disabled
@Slf4j
//@ExtendWith(MockitoExtension.class) 单元测试
@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = "spring.config.name=application-test") // 集成测试
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class productServiceTestDis {

    //@InjectMocks
    @InjectMocks
    private ProductService productService;

    @MockitoBean
    private RedisService redisService;
    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private RedisLockUtil redisLockUtil;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setup() {
        Mockito.doNothing().when(redisService).deleteCache("all_products");
        Mockito.when(redisLockUtil.tryLock(anyString(), anyString(), anyLong())).thenReturn(true);
    }


//    @Test
//    void testTryLock() {
//        when(redisLockUtil.tryLock(anyString(), anyString(), anyLong())).thenReturn(true);
//        boolean locked = redisLockUtil.tryLock("lock:add_product:Laptop", "randomUUID", 5000);
//        log.info("🚀 `testTryLock()` 结果: {}", locked);
//        assertTrue(locked);
//    }

    @Test
    public void addProduct() {

        when(redisLockUtil.tryLock(anyString(), anyString(), anyLong())).thenReturn(true);
        // 创建  Mock Product
        ProductDTO mockProduct = new ProductDTO(1L,"Laptop", 2000.0);

        // 创建 Product 实体
        Product mockSavedProduct = Product.builder()
                .id(1L)
                .name("Laptop")
                .price(2000.0)
                .build();

        when(productRepository.save(any(Product.class)))
                .thenReturn(mockSavedProduct);

        productService.addProduct(mockProduct);

        Mockito.verify(redisService).deleteCache("all_products");
        verify(redisService, Mockito.times(1)).deleteCache("all_products");
        verify(redisLockUtil, times(1)).unlock(anyString(), anyString()); //  确保 unlock() 被调用
    }

//    @BeforeEach
//    void setUp() {  // lenient().允许 UnnecessaryStubbingException 报错
//        lenient().when(redisLockUtil.tryLock(anyLong())).thenReturn(true); // 让锁始终成功
//    }

//
//    @Test
//    public void getAllProducts() {
//        // fake data
//        List<ProductDTO> mockProducts = Arrays.asList(
//                new ProductDTO("Product1", 100.0),
//                new ProductDTO("Product2", 200.0)
//        );
//
//        // Mock 缓存数据
//        //TypeReference<List<ProductDTO>> productListType = new TypeReference<>(){};
//        Mockito.doReturn(mockProducts)
//                .when(redisService).getCache(Mockito.eq("all_products"), Mockito.any(TypeReference.class));
//
//        // 调用方法
//        List<ProductDTO> result = productService.getAllProducts();
//
//        // 断言
//        assertNotNull(result);
//        assertEquals(2, result.size());
//    }
//
//    @Test
//    public void getProductById() {
//        // fake data
//        ProductDTO mockProduct = new ProductDTO("ProductA", 900.0);
//
//        // Mock 缓存数据
//        //TypeReference<ProductDTO> productType = new TypeReference<>(){};
//        Mockito.doReturn(mockProduct)
//                .when(redisService).getCache(Mockito.eq("product:1"), Mockito.any(TypeReference.class));
//
//        // 调用
//        ProductDTO result = productService.getProductById(1L);
//
//        // 断言
//        assertNotNull(result);
//        assertEquals("ProductA", result.getName());
//        assertEquals(900.0, result.getPrice());
//    }
//


//    @Test
//    void testConcurrentAddProduct() throws InterruptedException {
//        int threadCount = 10; // 并发线程数
//        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
//        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
//
//        for (int i = 0; i < threadCount; i++) {
//            executorService.execute(() -> {
//                try {
//                    ProductDTO productDTO = new ProductDTO("Concurrent Product", 50.0);
//                    productService.addProduct(productDTO); // 多线程并发执行
//                } catch (Exception e) {
//                    log.info("🚨################# addProduct() failed: {}", e.getMessage());
//                }finally {
//                    countDownLatch.countDown();
//                }
//            });
//        }
//
//        countDownLatch.await(); // 等待所有线程执行完成
//        executorService.shutdown();
//
//        // 确保 Redis 锁被调用
//        verify(redisLockUtil, atLeastOnce()).tryLock(anyString(),anyString(),anyLong());
//        verify(productRepository, atMost(1)).save(any(Product.class)); // 最多 1 次
//    }

//    @Test
//    void testRedisWrite() {
//        stringRedisTemplate.opsForValue().set("test_key", "test_value", 10, TimeUnit.SECONDS);
//        String value = stringRedisTemplate.opsForValue().get("test_key");
//        System.out.println("🚀 `testRedisWrite()` 结果：" + value);
//        assertEquals("test_value", value);
//    }

//    @Test
//    void testRedisConnection() {
//        try {
//            stringRedisTemplate.opsForValue().set("test_key", "test_value", 10, TimeUnit.SECONDS);
//            String value = stringRedisTemplate.opsForValue().get("test_key");
//            System.out.println("🚀 `testRedisConnection()` 结果：" + value);
//            assertEquals("test_value", value);
//        } catch (Exception e) {
//            System.out.println("🚨 Redis 连接失败：" + e.getMessage());
//            e.printStackTrace();
//        }
//    }
////
//    @Value("${spring.redis.host}")
//    private String redisHost;
//
//    @Test
//    void testRedisConfig() {
//        System.out.println("🚀 `testRedisConfig()` Redis Host：" + redisHost);
//        assertNotNull(redisHost);
//    }

}
