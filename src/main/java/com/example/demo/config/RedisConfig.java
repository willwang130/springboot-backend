package com.example.demo.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.password}")
    private String redisPassword;


    //  Redis 连接工厂
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        System.out.println("Redis 连接信息: " + redisHost + ":" + redisPort + " 密码: " + (redisPassword.isEmpty() ? "无密码" : "******"));
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        if (!redisPassword.isEmpty()) {
            config.setPassword(redisPassword); // 设置 Redis 密码
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    //  RedissonClient（用于 Bloom Filter）
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword)  // 设置密码（如果有）
                .setDatabase(0);
        return Redisson.create(config);
    }


//    // 操作 Redis
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(connectionFactory);
//
//        ObjectMapper objectMapper = JsonMapper.builder()
//                .addModule(new JavaTimeModule())  // 处理 LocalDateTime
//                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // 避免时间戳格式
//                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // 忽略未知字段
//                .serializationInclusion(JsonInclude.Include.NON_NULL) // 只序列化非 null 字段
//                .build();
//
//        // 直接在构造方法中传入 ObjectMapper
//        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
//
//        // Key 使用 String 序列化
//        template.setKeySerializer(new StringRedisSerializer());
//        // Value 使用 JSON 序列化
//        template.setValueSerializer(jsonSerializer);
//        template.setHashKeySerializer(new StringRedisSerializer());
//        template.setHashValueSerializer(jsonSerializer);
//
//        template.afterPropertiesSet();
//        return template;
//    }

}
