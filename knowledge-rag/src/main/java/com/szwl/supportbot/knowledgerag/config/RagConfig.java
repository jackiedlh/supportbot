package com.szwl.supportbot.knowledgerag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * RAG 配置类
 * 包含 RAG 相关的配置，支持从 Nacos 动态加载和热更新
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "")
public class RagConfig {

    /**
     * 系统提示词模板
     * 可以从 Nacos 配置中动态加载
     */
    private String systemPrompt;

    /**
     * 检索配置
     */
    private RetrievalConfig retrieval;

    /**
     * 检索配置类
     */
    @Data
    public static class RetrievalConfig {
        /**
         * 召回数量 - 从向量数据库检索的文档数量
         */
        private int topK = 5;
        
        /**
         * 相似度阈值 - 向量检索的相似度阈值
         */
        private double similarityThreshold = 0.7;
        
        /**
         * 最大token数 - 用于控制检索结果的总长度
         */
        private int maxTokens = 4000;
    }

        /**
     * 获取系统提示词模板
     */
    public String getSystemPromptTemplate() {
        return systemPrompt;
    }

    /**
     * 获取检索配置
     */
    public RetrievalConfig getRetrieval() {
        if (retrieval == null) {
            retrieval = new RetrievalConfig();
        }
        return retrieval;
    }
}
