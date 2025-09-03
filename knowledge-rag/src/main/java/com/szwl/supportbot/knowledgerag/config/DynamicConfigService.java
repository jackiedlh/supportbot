package com.szwl.supportbot.knowledgerag.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;

/**
 * 动态配置服务
 * 从 Nacos 动态获取和更新 RAG 配置
 */
@Slf4j
@Service
@RefreshScope
public class DynamicConfigService {

    @Autowired
    private NacosConfigManager nacosConfigManager;

    @Autowired
    private RagConfig ragConfig;

    private final ObjectMapper jsonObjectMapper = new ObjectMapper();

    /**
     * 获取RAG配置
     */
    public RagConfig getRagConfig() {
        try {
            // 从Nacos获取RAG配置
            String configContent = getConfigFromNacos();
            if (configContent != null && !configContent.trim().isEmpty()) {
                return parseRagConfig(configContent);
            }
        } catch (Exception e) {
            log.error("获取RAG配置失败", e);
        }
        return ragConfig; // 返回默认配置
    }

    /**
     * 从Nacos获取配置
     */
    private String getConfigFromNacos() {
        try {
            ConfigService configService = nacosConfigManager.getConfigService();
            String dataId = "rag-config.yaml";
            String group = "DEFAULT_GROUP";
            
            String config = configService.getConfig(dataId, group, 5000);
            log.info("从Nacos获取RAG配置成功: dataId={}, group={}", dataId, group);
            return config;
            
        } catch (NacosException e) {
            log.error("从Nacos获取RAG配置失败", e);
            return null;
        }
    }

    /**
     * 解析RAG配置
     * 自动识别YAML和JSON格式
     */
    private RagConfig parseRagConfig(String configContent) {
        if (configContent == null || configContent.trim().isEmpty()) {
            log.warn("RAG配置内容为空");
            return ragConfig;
        }
        
        log.info("开始解析RAG配置，内容长度: {}", configContent.length());
        log.debug("RAG配置内容: {}", configContent);
        
        try {
            // 尝试解析为YAML格式
            try {
                ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
                RagConfig yamlConfig = yamlMapper.readValue(configContent, new TypeReference<RagConfig>() {});
                log.info("成功解析YAML格式RAG配置: systemPrompt={}, retrieval={}", 
                        yamlConfig.getSystemPromptTemplate() != null ? "已获取" : "未获取",
                        yamlConfig.getRetrieval() != null ? "已获取" : "未获取");
                return yamlConfig;
            } catch (Exception yamlException) {
                log.debug("YAML解析失败，尝试JSON格式: {}", yamlException.getMessage());
                
                // 尝试解析为JSON格式
                try {
                    RagConfig jsonConfig = jsonObjectMapper.readValue(configContent, new TypeReference<RagConfig>() {});
                    log.info("成功解析JSON格式RAG配置: systemPrompt={}, retrieval={}", 
                            jsonConfig.getSystemPromptTemplate() != null ? "已获取" : "未获取",
                            jsonConfig.getRetrieval() != null ? "已获取" : "未获取");
                    return jsonConfig;
                } catch (Exception jsonException) {
                    log.error("JSON解析也失败", jsonException);
                    throw new RuntimeException("无法解析RAG配置文件，既不是有效的YAML也不是有效的JSON", jsonException);
                }
            }
        } catch (Exception e) {
            log.error("解析RAG配置失败", e);
            return ragConfig; // 返回默认配置
        }
    }

    /**
     * 监听配置变化
     */
    public void addConfigListener(Runnable onChangeCallback) {
        try {
            ConfigService configService = nacosConfigManager.getConfigService();
            String dataId = "rag-config.yaml";
            String group = "DEFAULT_GROUP";
            
            log.info("添加RAG配置监听器: dataId={}, group={}", dataId, group);
            
            configService.addListener(dataId, group, new Listener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("RAG配置发生变化: dataId={}, group={}", dataId, group);
                    try {
                        // 解析新配置
                        RagConfig newConfig = parseRagConfig(configInfo);
                        if (newConfig != null) {
                            // 应用新的RAG配置
                            updateRagConfig(newConfig);
                            log.info("RAG配置已应用");
                            
                            // 执行回调
                            if (onChangeCallback != null) {
                                onChangeCallback.run();
                            }
                        }
                    } catch (Exception e) {
                        log.error("处理RAG配置变化失败", e);
                    }
                }

                @Override
                public Executor getExecutor() {
                    return null; // 使用默认执行器
                }
            });
            
            log.info("RAG配置监听器已添加: dataId={}, group={}", dataId, group);
            
        } catch (NacosException e) {
            log.error("添加RAG配置监听器失败", e);
        }
    }

    /**
     * 更新RAG配置
     */
    private void updateRagConfig(RagConfig newConfig) {
        if (newConfig.getSystemPromptTemplate() != null) {
            ragConfig.setSystemPrompt(newConfig.getSystemPromptTemplate());
        }
        if (newConfig.getRetrieval() != null) {
            ragConfig.setRetrieval(newConfig.getRetrieval());
        }
    }

    /**
     * 获取系统提示词模板
     */
    public String getSystemPromptTemplate() {
        RagConfig config = getRagConfig();
        String systemPrompt = config.getSystemPromptTemplate();
        log.info("获取系统提示词模板: systemPrompt={}", systemPrompt != null ? "已获取，长度=" + systemPrompt.length() : "未获取到");
        return systemPrompt;
    }

    /**
     * 获取检索配置
     */
    public RagConfig.RetrievalConfig getRetrievalConfig() {
        RagConfig config = getRagConfig();
        return config.getRetrieval();
    }

    /**
     * 检查配置是否可用
     */
    public boolean isConfigAvailable() {
        try {
            String configContent = getConfigFromNacos();
            return configContent != null && !configContent.trim().isEmpty();
        } catch (Exception e) {
            log.error("检查RAG配置可用性失败", e);
            return false;
        }
    }
}