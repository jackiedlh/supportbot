package com.szwl.supportbot.knowledgerag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Knowledge RAG 主应用类
 * 基于 Nacos 配置中心的知识库检索问答系统
 */
@SpringBootApplication
@RefreshScope
@EnableDiscoveryClient
public class KnowledgeRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeRagApplication.class, args);
    }

    /**
     * 配置 RestTemplate Bean
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
