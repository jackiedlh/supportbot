package com.szwl.supportbot.generalchat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Chat 配置类
 * 包含 Chat 相关的配置，支持从 Nacos 动态加载和热更新
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "chat")
public class ChatConfig {

    /**
     * 系统提示词模板
     * 可以从 Nacos 配置中动态加载
     */
    private String systemPrompt;

    /**
     * 获取系统提示词模板
     */
    public String getSystemPrompt() {
        return systemPrompt;
    }
}
