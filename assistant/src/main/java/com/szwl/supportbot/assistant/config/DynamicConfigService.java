package com.szwl.supportbot.assistant.config;

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

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 动态配置服务
 * 从 Nacos 动态获取和更新业务类型配置
 */
@Slf4j
@Service
@RefreshScope
public class DynamicConfigService {

    @Autowired
    private NacosConfigManager nacosConfigManager;

    @Autowired
    private AgentConfig agentConfig;

    private final ObjectMapper jsonObjectMapper = new ObjectMapper();
    private final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());

    /**
     * 获取业务类型配置
     */
    public AgentConfig.BusinessTypeConfig getBusinessTypeConfig(String businessType) {
        try {
            // 从Nacos获取业务类型配置
            String configContent = getConfigFromNacos(businessType);
            if (configContent != null && !configContent.trim().isEmpty()) {
                return parseBusinessTypeConfig(configContent);
            }
        } catch (Exception e) {
            log.error("获取业务类型配置失败: {}", businessType, e);
        }
        return null;
    }

    /**
     * 从Nacos获取配置
     */
    private String getConfigFromNacos(String businessType) {
        try {
            ConfigService configService = nacosConfigManager.getConfigService();
            String dataId = businessType + "-agent-config.yaml";
            String group = "DEFAULT_GROUP";
            
            String config = configService.getConfig(dataId, group, 5000);
            log.info("从Nacos获取配置成功: dataId={}, group={}", dataId, group);
            return config;
            
        } catch (NacosException e) {
            log.error("从Nacos获取配置失败: {}", businessType, e);
            return null;
        }
    }

    /**
     * 解析业务类型配置
     * 自动识别YAML和JSON格式
     */
    private AgentConfig.BusinessTypeConfig parseBusinessTypeConfig(String configContent) {
        if (configContent == null || configContent.trim().isEmpty()) {
            log.warn("配置内容为空");
            return null;
        }
        
        try {
            // 尝试解析为YAML格式
            try {
                ObjectMapper yamlMapper = new ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
                AgentConfig.BusinessTypeConfig yamlConfig = yamlMapper.readValue(configContent, new TypeReference<AgentConfig.BusinessTypeConfig>() {});
                log.info("成功解析YAML格式配置");
                return yamlConfig;
            } catch (Exception yamlException) {
                log.debug("YAML解析失败，尝试JSON格式: {}", yamlException.getMessage());
                
                // 尝试解析为JSON格式
                try {
                    AgentConfig.BusinessTypeConfig jsonConfig = jsonObjectMapper.readValue(configContent, new TypeReference<AgentConfig.BusinessTypeConfig>() {});
                    log.info("成功解析JSON格式配置");
                    return jsonConfig;
                } catch (Exception jsonException) {
                    log.error("JSON解析也失败", jsonException);
                    throw new RuntimeException("无法解析配置文件，既不是有效的YAML也不是有效的JSON", jsonException);
                }
            }
        } catch (Exception e) {
            log.error("解析业务类型配置失败", e);
            return null;
        }
    }

    /**
     * 监听配置变化
     */
    public void addConfigListener(String businessType, Runnable onChangeCallback) {
        try {
            ConfigService configService = nacosConfigManager.getConfigService();
            String dataId = businessType + "-agent-config.yaml";
            String group = "DEFAULT_GROUP";
            
            log.info("添加配置监听器: dataId={}, group={}", dataId, group);
            
            configService.addListener(dataId, group, new Listener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("配置发生变化: dataId={}, group={}", dataId, group);
                    try {
                        // 解析新配置
                        AgentConfig.BusinessTypeConfig newConfig = parseBusinessTypeConfig(configInfo);
                        if (newConfig != null) {
                            // 应用新的业务类型配置
                            agentConfig.updateBusinessTypeConfig(businessType, newConfig);
                            log.info("业务类型配置已应用: {}", businessType);
                            
                            // 执行回调
                            if (onChangeCallback != null) {
                                onChangeCallback.run();
                            }
                        }
                    } catch (Exception e) {
                        log.error("处理配置变化失败: {}", businessType, e);
                    }
                }

                @Override
                public Executor getExecutor() {
                    return null; // 使用默认执行器
                }
            });
            
            log.info("配置监听器已添加: dataId={}, group={}", dataId, group);
            
        } catch (NacosException e) {
            log.error("添加配置监听器失败: {}", businessType, e);
        }
    }

    /**
     * 获取系统提示词
     */
    public String getSystemPrompt(String businessType) {
        if (businessType == null || businessType.trim().isEmpty()) {
            return agentConfig.getSystemPrompt();
        }
        
        AgentConfig.BusinessTypeConfig businessConfig = getBusinessTypeConfig(businessType);
        if (businessConfig != null && businessConfig.getSystemPrompt() != null && !businessConfig.getSystemPrompt().trim().isEmpty()) {
            return businessConfig.getSystemPrompt();
        }
        
        return agentConfig.getSystemPrompt();
    }

    /**
     * 获取MCP配置
     */
    public AgentConfig.McpConfig getMcpConfig(String businessType) {
        if (businessType == null || businessType.trim().isEmpty()) {
            return agentConfig.getMcp();
        }
        
        AgentConfig.BusinessTypeConfig businessConfig = getBusinessTypeConfig(businessType);
        if (businessConfig != null && businessConfig.getMcp() != null) {
            return businessConfig.getMcp();
        }
        
        return agentConfig.getMcp();
    }

    /**
     * 获取业务配置
     */
    public Map<String, Object> getBusinessConfig(String businessType) {
        if (businessType == null || businessType.trim().isEmpty()) {
            return Map.of();
        }
        
        AgentConfig.BusinessTypeConfig businessConfig = getBusinessTypeConfig(businessType);
        if (businessConfig != null && businessConfig.getBusiness() != null) {
            return businessConfig.getBusiness();
        }
        
        return Map.of();
    }

    /**
     * 检查配置是否可用
     */
    public boolean isConfigAvailable(String businessType) {
        try {
            if (businessType == null || businessType.trim().isEmpty()) {
                return true; // 默认配置总是可用的
            }
            
            AgentConfig.BusinessTypeConfig config = getBusinessTypeConfig(businessType);
            return config != null;
            
        } catch (Exception e) {
            log.error("检查配置可用性失败: {}", businessType, e);
            return false;
        }
    }

    /**
     * 获取所有可用的业务类型
     */
    public java.util.Set<String> getAvailableBusinessTypes() {
        try {
            // 返回预定义的业务类型集合
            return java.util.Set.of("ecommerce", "customer_service", "sales");
        } catch (Exception e) {
            log.error("获取可用业务类型失败", e);
            return java.util.Set.of();
        }
    }
}
