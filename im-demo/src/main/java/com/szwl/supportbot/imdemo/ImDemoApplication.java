package com.szwl.supportbot.imdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * IM Demo 主应用类
 * 提供HTTP服务，注册到Nacos
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ImDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImDemoApplication.class, args);
    }

    /**
     * 配置 RestTemplate Bean，用于调用问题分类模块接口
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 配置CORS策略，允许前端跨域访问
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // 允许所有路径
                        .allowedOrigins("http://127.0.0.1:8081", "http://localhost:8081") // 允许前端的源
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的HTTP方法
                        .allowedHeaders("*") // 允许所有请求头
                        .allowCredentials(true) // 允许发送Cookie等凭证
                        .maxAge(3600); // 预检请求的缓存时间
            }
        };
    }
}
