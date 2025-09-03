package com.szwl.supportbot.generalchat.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.Executor;

/**
 * 动态配置服务
 * 负责从 Nacos 动态获取和刷新 Chat 配置
 * 参考 RAG 项目的实现方式
 */
@Slf4j
@Service
@RefreshScope
public class DynamicConfigService {

    @Autowired
    private NacosConfigManager nacosConfigManager;

    @Autowired
    private ChatConfig chatConfig; // 注入的ChatConfig

    private final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());

    private static final String CHAT_CONFIG_DATA_ID = "chat-config.yaml";
    private static final String DEFAULT_GROUP = "DEFAULT_GROUP";

    /**
     * 初始化配置加载和监听
     */
    @PostConstruct
    public void init() {
        // 初始加载配置
        loadAndListenConfig();
    }

    /**
     * 获取系统提示词模板
     */
    public String getSystemPromptTemplate() {
        return chatConfig.getSystemPrompt();
    }

    /**
     * 加载配置并添加监听器
     */
    private void loadAndListenConfig() {
        try {
            ConfigService configService = nacosConfigManager.getConfigService();
            
            // 初始加载配置
            String configContent = configService.getConfig(CHAT_CONFIG_DATA_ID, DEFAULT_GROUP, 5000);
            if (configContent != null) {
                updateChatConfig(configContent);
                log.info("Chat配置初始加载成功: dataId={}, group={}", CHAT_CONFIG_DATA_ID, DEFAULT_GROUP);
            } else {
                log.warn("Chat配置初始加载失败，使用默认配置: dataId={}, group={}", CHAT_CONFIG_DATA_ID, DEFAULT_GROUP);
            }
            
            // 注册配置变更监听器
            addConfigListener();
            
        } catch (NacosException e) {
            log.error("Chat配置加载或监听失败: dataId={}, group={}", CHAT_CONFIG_DATA_ID, DEFAULT_GROUP, e);
        }
    }

    /**
     * 添加配置监听器
     */
    private void addConfigListener() {
        try {
            ConfigService configService = nacosConfigManager.getConfigService();
            String dataId = CHAT_CONFIG_DATA_ID;
            String group = DEFAULT_GROUP;
            
            log.info("添加Chat配置监听器: dataId={}, group={}", dataId, group);
            
            configService.addListener(dataId, group, new Listener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("Chat配置发生变化: dataId={}, group={}", dataId, group);
                    try {
                        // 解析新配置
                        ChatConfig newConfig = parseChatConfig(configInfo);
                        if (newConfig != null) {
                            // 应用新配置
                            updateChatConfig(configInfo);
                            log.info("Chat配置应用成功: systemPromptLength={}", 
                                    chatConfig.getSystemPrompt().length());
                        } else {
                            log.warn("Chat配置解析失败，保持当前配置");
                        }
                    } catch (Exception e) {
                        log.error("Chat配置更新失败", e);
                    }
                }

                @Override
                public Executor getExecutor() {
                    return null; // 使用默认执行器
                }
            });
            
            log.info("Chat配置监听器已添加: dataId={}, group={}", dataId, group);
            
        } catch (NacosException e) {
            log.error("添加Chat配置监听器失败: dataId={}, group={}", CHAT_CONFIG_DATA_ID, DEFAULT_GROUP, e);
        }
    }

    /**
     * 解析Chat配置
     */
    private ChatConfig parseChatConfig(String configContent) {
        try {
            log.info("开始解析Chat配置: contentLength={}", configContent.length());
            ChatConfig config = yamlObjectMapper.readValue(configContent, ChatConfig.class);
            log.info("Chat配置解析成功: systemPromptLength={}", 
                    config.getSystemPrompt() != null ? config.getSystemPrompt().length() : 0);
            return config;
        } catch (Exception e) {
            log.error("Chat配置解析失败: content={}, error={}", configContent, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 更新Chat配置
     */
    private void updateChatConfig(String configContent) {
        try {
            ChatConfig newConfig = parseChatConfig(configContent);
            if (newConfig != null) {
                chatConfig.setSystemPrompt(newConfig.getSystemPrompt());
                log.info("ChatConfig已更新: systemPromptLength={}", 
                        chatConfig.getSystemPrompt().length());
            }
        } catch (Exception e) {
            log.error("更新ChatConfig失败", e);
        }
    }
}
