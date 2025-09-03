package com.szwl.supportbot.assistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Agent 通用配置类
 * 包含通用的 Agent 配置和 MCP 配置结构
 * 支持从 Nacos 动态加载和热更新
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "agent")
public class AgentConfig {

    /**
     * 系统提示词
     * 可以从 Nacos 配置中动态加载
     */
    private String systemPrompt;

    /**
     * 通用 MCP 配置
     * 具体的业务类型配置通过 DynamicConfigService 从 Nacos 动态加载
     */
    private McpConfig mcp;

    /**
     * 动态业务类型配置
     * 由 DynamicConfigService 动态更新
     */
    private Map<String, BusinessTypeConfig> businessTypeConfigs;

    /**
     * 更新业务类型配置
     * 由 DynamicConfigService 调用
     */
    public void updateBusinessTypeConfig(String businessType, BusinessTypeConfig config) {
        if (businessTypeConfigs == null) {
            businessTypeConfigs = new java.util.HashMap<>();
        }
        businessTypeConfigs.put(businessType, config);
    }

    /**
     * 获取业务类型配置
     */
    public BusinessTypeConfig getBusinessTypeConfig(String businessType) {
        return businessTypeConfigs != null ? businessTypeConfigs.get(businessType) : null;
    }

    /**
     * 获取业务类型的系统提示词
     * 优先从业务类型特定配置获取，如果没有则使用通用配置
     */
    public String getSystemPrompt(String businessType) {
        if (businessType != null && businessTypeConfigs != null) {
            BusinessTypeConfig config = businessTypeConfigs.get(businessType);
            if (config != null && config.getSystemPrompt() != null && !config.getSystemPrompt().trim().isEmpty()) {
                return config.getSystemPrompt();
            }
        }
        return this.systemPrompt;
    }

    /**
     * 业务类型配置类
     */
    @Data
    public static class BusinessTypeConfig {
        private String systemPrompt;
        private McpConfig mcp;
        private Map<String, Object> business;
    }

    /**
     * MCP 配置类
     */
    @Data
    public static class McpConfig {
        private Client client;
    }

    /**
     * MCP 客户端配置
     */
    @Data
    public static class Client {
        private Sse sse;
        private ToolCallback toolcallback;
    }

    /**
     * 工具回调配置
     */
    @Data
    public static class ToolCallback {
        private boolean enabled = true;
        private Map<String, Object> options;
    }

    /**
     * SSE 连接配置
     */
    @Data
    public static class Sse {
        private Map<String, Connection> connections;
    }

    /**
     * 连接配置
     */
    @Data
    public static class Connection {
        private String url;
        private String apiKey;
        private Map<String, String> params;
    }
}
