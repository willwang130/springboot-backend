package com.example.demo;

import com.example.demo.repository.UserRepository;
import com.example.demo.service.RedisService;
import com.example.demo.util.RedisLockUtil;
import com.example.demo.util.RedisUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

@TestConfiguration
public class TestConfig {

//    @MockitoBean
//    private RedisLockUtil redisLockUtil;
//
//    @MockitoBean
//    private RedisUtil redisUtil;
//
//    @Bean
//    public UserRepository userRepository() {
//        return Mockito.mock(UserRepository.class);  // 让 Spring 创建一个 Mock UserRepository
//    }



    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());  // 允许所有请求
        return http.build();
    }

//
//    @Bean
//    public RedisService redisService(RedisUtil redisUtil, RedisLockUtil redisLockUtil) {
//        return new RedisService(redisUtil, redisLockUtil);
//    }
//
//    @Bean
//    public DataSource dataSource() {
//        return DataSourceBuilder.create()
//                .driverClassName("org.h2.Driver")  // 这里用 H2 作为一个 "假" 数据源
//                .url("jdbc:h2:mem:testdb")  // 使用 H2 内存数据库，测试时不会连接 MySQL
//                .username("sa")
//                .password("")
//                .build();
//    }
}
