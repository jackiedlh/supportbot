package com.szwl.supportbot.questionclassifier.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 问题分类配置类
 * 支持从Nacos配置中心动态加载分类到主题的映射关系
 * 实现真正的配置驱动，支持动态扩展和实时同步
 */
@Slf4j
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "question-categories")
public class QuestionCategoryConfig {

    /**
     * 分类映射配置
     * key: 分类关键词，value: 对应的主题
     */
    private Map<String, String> categoryMappings = new HashMap<>();

    /**
     * 默认主题
     */
    private String defaultTopic = "supportbot.general.chat";

    /**
     * 配置初始化后的回调
     */
    @PostConstruct
    public void init() {
        log.info("=== QuestionCategoryConfig 初始化开始 ===");
        log.info("当前映射数量: {}", categoryMappings.size());
        log.info("默认主题: {}", defaultTopic);
        log.info("categoryMappings 对象: {}", categoryMappings);
        log.info("categoryMappings 是否为 null: {}", categoryMappings == null);
        log.info("categoryMappings 类型: {}", categoryMappings != null ? categoryMappings.getClass().getName() : "null");
        
        // 检查是否有配置绑定
        if (categoryMappings != null && !categoryMappings.isEmpty()) {
            log.info("配置绑定成功，映射详情:");
            categoryMappings.forEach((key, value) -> log.info("  {} -> {}", key, value));
        } else {
            log.warn("配置绑定失败！categoryMappings 为空");
            log.warn("请检查 Nacos 配置中心是否有 question-categories.yml 文件");
            log.warn("或者检查配置前缀 'question-categories' 是否正确");
            
            // 添加更多调试信息
            log.warn("=== 调试信息 ===");
            log.warn("1. 检查 Nacos 配置文件名: question-categories.yml");
            log.warn("2. 检查配置前缀: question-categories");
            log.warn("3. 检查配置格式是否正确（中文键需要引号）");
            log.warn("4. 检查 Nacos 连接是否正常");
            log.warn("5. 检查配置是否成功推送");
        }
        
        logConfigInfo();
        log.info("=== QuestionCategoryConfig 初始化完成 ===");
    }

    /**
     * 配置刷新后的回调
     * 当Nacos配置变更时，Spring会自动调用此方法
     */
    public void refresh() {
        log.info("问题分类配置已刷新，当前映射数量: {}", categoryMappings.size());
        logConfigInfo();
    }

    /**
     * 根据分类名称获取对应的主题
     * 通过配置的映射关系动态判断，支持热更新
     * 
     * @param category 分类名称
     * @return 对应的主题
     */
    public String getTopicForCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            log.debug("分类名称为空，使用默认主题: {}", defaultTopic);
            return defaultTopic;
        }

        String cleanCategory = category.trim();
        log.debug("查找分类 '{}' 对应的主题", cleanCategory);
        
        // 遍历配置的映射关系，查找匹配的分类
        for (Map.Entry<String, String> entry : categoryMappings.entrySet()) {
            String keyword = entry.getKey();
            String topic = entry.getValue();
            
            // 如果分类名称包含配置的关键词，返回对应的主题
            if (cleanCategory.contains(keyword)) {
                log.debug("分类 '{}' 匹配关键词 '{}'，路由到主题: {}", cleanCategory, keyword, topic);
                return topic;
            }
        }

        // 未找到匹配的分类，返回默认主题
        log.debug("分类 '{}' 未找到匹配，使用默认主题: {}", cleanCategory, defaultTopic);
        return defaultTopic;
    }

    /**
     * 获取所有分类映射（只读）
     * 
     * @return 分类映射Map的副本
     */
    public Map<String, String> getAllCategoryMappings() {
        return new HashMap<>(categoryMappings);
    }

    /**
     * 获取配置统计信息
     * 
     * @return 配置统计信息
     */
    public String getConfigStats() {
        return String.format("分类映射数量: %d, 默认主题: %s", 
            categoryMappings.size(), defaultTopic);
    }

    /**
     * 记录配置信息到日志
     */
    private void logConfigInfo() {
        log.info("=== 问题分类配置信息 ===");
        log.info("分类映射数量: {}", categoryMappings.size());
        
        if (!categoryMappings.isEmpty()) {
            log.info("分类映射详情:");
            categoryMappings.forEach((keyword, topic) -> 
                log.info("  {} -> {}", keyword, topic));
        }
        log.info("========================");
    }
}
