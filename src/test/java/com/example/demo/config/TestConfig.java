package com.example.demo.config;

import com.example.demo.service.RedisService;
import com.example.demo.util.RedisLockUtil;
import com.example.demo.util.RedisUtil;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

@Configuration
@ComponentScan("com.example.demo.service") // 让 Spring 识别 ProductService
public class TestConfig {

    @MockitoBean
    private RedisLockUtil redisLockUtil;

    @MockitoBean
    private RedisUtil redisUtil;


    @Bean
    public RedisService redisService(RedisUtil redisUtil, RedisLockUtil redisLockUtil) {
        return new RedisService(redisUtil, redisLockUtil);
    }

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")  // 这里用 H2 作为一个 "假" 数据源
                .url("jdbc:h2:mem:testdb")  // 使用 H2 内存数据库，测试时不会连接 MySQL
                .username("sa")
                .password("")
                .build();
    }
}
