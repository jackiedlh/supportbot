package com.szwl.supportbot.assistant.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 * 手动创建 RedisTemplate Bean 以确保可用性
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisConfig {

    public RedisConfig() {
        log.info("=== RedisConfig 初始化开始 ===");
        log.info("Redis 配置类已加载");
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("创建 RedisTemplate Bean");
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 设置 key 序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // 设置 value 序列化器
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        // 初始化
        template.afterPropertiesSet();
        
        log.info("RedisTemplate Bean 创建成功");
        return template;
    }
}
